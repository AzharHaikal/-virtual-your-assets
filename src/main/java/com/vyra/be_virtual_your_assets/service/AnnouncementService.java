package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.AnnouncementStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.announcement.AnnouncementRequest;
import com.vyra.be_virtual_your_assets.dto.announcement.AnnouncementResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.entity.Announcement;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.AnnouncementRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Announcements from VYRA. A published one is a banner in the app during its period and is sent once as
 * an ANNOUNCEMENT notification (inbox, and push if the member wants it) to the active members of its
 * language. Changing it after it was sent does not change the notifications already sent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {

    static final int MAX_PAGE_SIZE = 100;
    static final int DELIVERY_BATCH = 500;

    private final AnnouncementRepository announcementRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final ValidationService validationService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final AdminActivityService adminActivityService;
    private final TimeService timeService;

    // =========================================================================
    // Member
    // =========================================================================

    /** Active announcements of the member's language, newest first (banner). */
    @Transactional(readOnly = true)
    public BaseResponse<List<AnnouncementResponse>> getActive(String memberId) {
        Member member = validationService.getMemberById(memberId);
        String language = member.getLanguage() != null ? member.getLanguage().name() : Announcement.ALL_LANGUAGES;
        LocalDateTime now = timeService.nowUtc();
        List<AnnouncementResponse> items = announcementRepository.findActive(now, language).stream()
                .map(a -> toResponse(a, now, false))
                .toList();
        return new BaseResponse<>(ErrorConstant.ANNOUNCEMENT_LIST_SUCCESS.getCode(),
                ErrorConstant.ANNOUNCEMENT_LIST_SUCCESS.getMessage(), items);
    }

    // =========================================================================
    // Portal
    // =========================================================================

    @Transactional(readOnly = true)
    public BaseResponse<PageResponse<AnnouncementResponse>> getAll(int page, int size) {
        adminAuthorizationService.requirePermission(AdminPermission.SETTINGS_VIEW);
        LocalDateTime now = timeService.nowUtc();
        Page<Announcement> announcements = announcementRepository.findByDeletedAtIsNullOrderByCreatedAtDesc(
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE)));
        return new BaseResponse<>(ErrorConstant.ANNOUNCEMENT_LIST_SUCCESS.getCode(),
                ErrorConstant.ANNOUNCEMENT_LIST_SUCCESS.getMessage(),
                PageResponse.from(announcements.map(a -> toResponse(a, now, true))));
    }

    /** Saved as a draft. */
    @Transactional
    public BaseResponse<AnnouncementResponse> create(AnnouncementRequest request, CustomAdminDetails admin) {
        adminAuthorizationService.requirePermission(AdminPermission.ANNOUNCEMENT_MANAGE);
        Announcement announcement = new Announcement();
        apply(announcement, request);
        announcement.setCreatedAt(timeService.nowUtc());
        announcement.setCreatedBy(admin.getAdminId());
        announcementRepository.save(announcement);
        audit(admin, AdminActivityEvent.CREATE_ANNOUNCEMENT, announcement);
        return saved(announcement);
    }

    @Transactional
    public BaseResponse<AnnouncementResponse> update(String id, AnnouncementRequest request, CustomAdminDetails admin) {
        adminAuthorizationService.requirePermission(AdminPermission.ANNOUNCEMENT_MANAGE);
        Announcement announcement = find(id);
        apply(announcement, request);
        touch(announcement, admin);
        announcementRepository.save(announcement);
        audit(admin, AdminActivityEvent.UPDATE_ANNOUNCEMENT, announcement);
        return saved(announcement);
    }

    /** Shown from its start; notifications are sent by AnnouncementJob once it is active. */
    @Transactional
    public BaseResponse<AnnouncementResponse> publish(String id, CustomAdminDetails admin) {
        adminAuthorizationService.requirePermission(AdminPermission.ANNOUNCEMENT_MANAGE);
        Announcement announcement = find(id);
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            announcement.setStatus(AnnouncementStatus.PUBLISHED);
            announcement.setPublishedAt(timeService.nowUtc());
            touch(announcement, admin);
            announcementRepository.save(announcement);
            audit(admin, AdminActivityEvent.PUBLISH_ANNOUNCEMENT, announcement);
        }
        return saved(announcement);
    }

    /** Taken down: no banner any more; notifications already sent stay in the inboxes. */
    @Transactional
    public BaseResponse<AnnouncementResponse> unpublish(String id, CustomAdminDetails admin) {
        adminAuthorizationService.requirePermission(AdminPermission.ANNOUNCEMENT_MANAGE);
        Announcement announcement = find(id);
        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            announcement.setStatus(AnnouncementStatus.UNPUBLISHED);
            touch(announcement, admin);
            announcementRepository.save(announcement);
            audit(admin, AdminActivityEvent.UNPUBLISH_ANNOUNCEMENT, announcement);
        }
        return saved(announcement);
    }

    @Transactional
    public BaseResponse<Void> delete(String id, CustomAdminDetails admin) {
        adminAuthorizationService.requirePermission(AdminPermission.ANNOUNCEMENT_MANAGE);
        Announcement announcement = find(id);
        announcement.setDeletedAt(timeService.nowUtc());
        touch(announcement, admin);
        announcementRepository.save(announcement);
        audit(admin, AdminActivityEvent.DELETE_ANNOUNCEMENT, announcement);
        return new BaseResponse<>(ErrorConstant.ANNOUNCEMENT_DELETE_SUCCESS.getCode(),
                ErrorConstant.ANNOUNCEMENT_DELETE_SUCCESS.getMessage(), null);
    }

    // =========================================================================
    // Delivery (AnnouncementJob)
    // =========================================================================

    /**
     * Sends every active, not yet sent announcement to the active members of its language. Idempotent per
     * member (dedup key ANNOUNCEMENT:{id}:{memberId}), so a run that stops halfway is completed by the next.
     *
     * @return notifications created
     */
    public int deliverDue(LocalDateTime now) {
        int created = 0;
        for (Announcement announcement : announcementRepository.findToDeliver(now)) {
            created += deliver(announcement);
            announcement.setDeliveredAt(timeService.nowUtc());
            announcementRepository.save(announcement);
            log.info("Announcement delivered. announcementId: {}", announcement.getId());
        }
        return created;
    }

    private int deliver(Announcement announcement) {
        NotificationMessages.Message message = new NotificationMessages.Message(
                StringUtils.abbreviate(announcement.getTitle(), NotificationMessages.MAX_TITLE),
                StringUtils.abbreviate(announcement.getBody(), NotificationMessages.MAX_BODY));
        Map<String, String> payload = Map.of("route", "announcement", "announcementId", announcement.getId());
        int created = 0;
        int page = 0;
        Page<Member> members;
        do {
            members = memberRepository.findByStatusOrderByIdAsc(ActiveStatus.ACTIVE, PageRequest.of(page++, DELIVERY_BATCH));
            for (Member member : members) {
                if (!matchesLanguage(announcement, member)) {
                    continue;
                }
                try {
                    if (notificationService.notify(member, NotificationType.ANNOUNCEMENT,
                            "ANNOUNCEMENT:" + announcement.getId() + ":" + member.getId(), message, payload)) {
                        created++;
                    }
                } catch (RuntimeException e) {
                    log.warn("Announcement notification failed. memberId: {} reason: {}",
                            member.getId(), e.getClass().getSimpleName());
                }
            }
        } while (members.hasNext());
        return created;
    }

    static boolean matchesLanguage(Announcement announcement, Member member) {
        return Announcement.ALL_LANGUAGES.equals(announcement.getLanguage())
                || (member.getLanguage() != null && member.getLanguage().name().equals(announcement.getLanguage()));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void apply(Announcement announcement, AnnouncementRequest request) {
        LocalDateTime startsAt = timeService.parseClientDateTime(request.getStartsAt(), timeService.defaultZone());
        LocalDateTime endsAt = StringUtils.isBlank(request.getEndsAt()) ? null
                : timeService.parseClientDateTime(request.getEndsAt(), timeService.defaultZone());
        if (endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new BadRequestException(ErrorConstant.ANNOUNCEMENT_PERIOD_INVALID);
        }
        announcement.setTitle(request.getTitle().trim());
        announcement.setBody(request.getBody().strip());
        announcement.setLanguage(StringUtils.defaultIfBlank(request.getLanguage(), Announcement.ALL_LANGUAGES));
        announcement.setStartsAt(startsAt);
        announcement.setEndsAt(endsAt);
    }

    private Announcement find(String id) {
        return announcementRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.ANNOUNCEMENT_NOT_FOUND));
    }

    private void touch(Announcement announcement, CustomAdminDetails admin) {
        announcement.setUpdatedAt(timeService.nowUtc());
        announcement.setModifiedBy(admin.getAdminId());
    }

    private void audit(CustomAdminDetails admin, AdminActivityEvent event, Announcement announcement) {
        adminActivityService.createAdminActivity(admin.getAdminId(), null, event, announcement.getTitle(), null, null);
    }

    private BaseResponse<AnnouncementResponse> saved(Announcement announcement) {
        return new BaseResponse<>(ErrorConstant.ANNOUNCEMENT_SAVE_SUCCESS.getCode(),
                ErrorConstant.ANNOUNCEMENT_SAVE_SUCCESS.getMessage(), toResponse(announcement, timeService.nowUtc(), true));
    }

    static AnnouncementResponse toResponse(Announcement announcement, LocalDateTime now, boolean forPortal) {
        AnnouncementResponse response = new AnnouncementResponse();
        response.setId(announcement.getId());
        response.setTitle(announcement.getTitle());
        response.setBody(announcement.getBody());
        response.setLanguage(announcement.getLanguage());
        response.setStatus(announcement.getStatus());
        response.setStartsAt(announcement.getStartsAt());
        response.setEndsAt(announcement.getEndsAt());
        response.setPublishedAt(announcement.getPublishedAt());
        response.setDeliveredAt(forPortal ? announcement.getDeliveredAt() : null);
        response.setActive(announcement.isActiveAt(now));
        return response;
    }
}
