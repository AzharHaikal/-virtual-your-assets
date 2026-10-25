package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.constant.RolePermissionMapper;
import com.vyra.be_virtual_your_assets.constant.UserReportStatus;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.report.user.CreateUserReportRequest;
import com.vyra.be_virtual_your_assets.dto.report.user.UpdateUserReportRequest;
import com.vyra.be_virtual_your_assets.dto.report.user.UserReportResponse;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.UserReport;
import com.vyra.be_virtual_your_assets.entity.UserReportAttachment;
import com.vyra.be_virtual_your_assets.entity.UserReportNote;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.UserReportAttachmentRepository;
import com.vyra.be_virtual_your_assets.repository.UserReportNoteRepository;
import com.vyra.be_virtual_your_assets.repository.UserReportRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Problems and abuse reported by members. Admins with REPORT_HANDLE work the queue (status, assignee,
 * notes); a report shows what the member wrote and how to reach them, never their private data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserReportService {

    static final int MAX_PER_DAY = 5;
    static final int MAX_PAGE_SIZE = 100;

    private final UserReportRepository reportRepository;
    private final UserReportNoteRepository noteRepository;
    private final UserReportAttachmentRepository attachmentRepository;
    private final MemberRepository memberRepository;
    private final AdminRepository adminRepository;
    private final ValidationService validationService;
    private final NotificationService notificationService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final AdminActivityService adminActivityService;
    private final TimeService timeService;

    // =========================================================================
    // Member
    // =========================================================================

    /** At most 5 per day; the screenshot must be a real JPEG / PNG / WebP of at most 1 MB. */
    @Transactional
    public BaseResponse<UserReportResponse> create(String memberId, CreateUserReportRequest request) {
        validationService.getMemberById(memberId);
        LocalDateTime now = timeService.nowUtc();
        if (reportRepository.countByMemberIdAndCreatedAtAfter(memberId, now.minusDays(1)) >= MAX_PER_DAY) {
            throw new TooManyRequestsException(ErrorConstant.USER_REPORT_TOO_MANY, Duration.ofHours(1).getSeconds());
        }
        byte[] image = decodeAttachment(request.getAttachmentBase64());

        UserReport report = new UserReport();
        report.setMemberId(memberId);
        report.setCategory(request.getCategory());
        report.setDescription(request.getDescription().strip());
        report.setHasAttachment(image != null);
        report.setAppVersion(StringUtils.trimToNull(request.getAppVersion()));
        report.setPlatform(StringUtils.trimToNull(request.getPlatform()));
        report.setCreatedAt(now);
        reportRepository.saveAndFlush(report);
        if (image != null) {
            UserReportAttachment attachment = new UserReportAttachment();
            attachment.setReportId(report.getId());
            attachment.setContent(image);
            attachment.setContentType(AccountService.imageType(image));
            attachmentRepository.save(attachment);
        }
        log.info("User report created. reportId: {} category: {}", report.getId(), report.getCategory());
        return new BaseResponse<>(ErrorConstant.USER_REPORT_CREATED.getCode(),
                ErrorConstant.USER_REPORT_CREATED.getMessage(), toMemberResponse(report));
    }

    /** The member's own reports, newest first (latest 50). */
    @Transactional(readOnly = true)
    public BaseResponse<List<UserReportResponse>> getOwn(String memberId) {
        return new BaseResponse<>(ErrorConstant.USER_REPORT_LIST_SUCCESS.getCode(),
                ErrorConstant.USER_REPORT_LIST_SUCCESS.getMessage(),
                reportRepository.findTop50ByMemberIdOrderByCreatedAtDesc(memberId).stream()
                        .map(UserReportService::toMemberResponse).toList());
    }

    // =========================================================================
    // Portal (REPORT_HANDLE)
    // =========================================================================

    @Transactional(readOnly = true)
    public BaseResponse<PageResponse<UserReportResponse>> getQueue(UserReportStatus status, int page, int size) {
        adminAuthorizationService.requirePermission(AdminPermission.REPORT_HANDLE);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<UserReport> reports = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc(pageable)
                : reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        Map<String, Member> members = memberRepository.findByIdIn(
                        reports.getContent().stream().map(UserReport::getMemberId).distinct().toList()).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
        Map<String, String> admins = adminNames(reports.getContent().stream().map(UserReport::getAssignedAdminId));
        return new BaseResponse<>(ErrorConstant.USER_REPORT_LIST_SUCCESS.getCode(),
                ErrorConstant.USER_REPORT_LIST_SUCCESS.getMessage(),
                PageResponse.from(reports.map(r -> toPortalResponse(r, members.get(r.getMemberId()), admins, null))));
    }

    @Transactional(readOnly = true)
    public BaseResponse<UserReportResponse> getDetail(String reportId) {
        adminAuthorizationService.requirePermission(AdminPermission.REPORT_HANDLE);
        return detail(find(reportId), ErrorConstant.USER_REPORT_DETAIL_SUCCESS);
    }

    @Transactional(readOnly = true)
    public UserReportAttachment getAttachment(String reportId) {
        adminAuthorizationService.requirePermission(AdminPermission.REPORT_HANDLE);
        return attachmentRepository.findById(find(reportId).getId())
                .orElseThrow(() -> new NotFoundException(ErrorConstant.USER_REPORT_NOT_FOUND));
    }

    /** Changes status / assignee and records them with the note in the history. Resolving tells the member. */
    @Transactional
    public BaseResponse<UserReportResponse> update(String reportId, UpdateUserReportRequest request,
                                                   CustomAdminDetails admin) {
        adminAuthorizationService.requirePermission(AdminPermission.REPORT_HANDLE);
        UserReport report = find(reportId);
        LocalDateTime now = timeService.nowUtc();
        UserReportStatus newStatus = request.getStatus() != null && request.getStatus() != report.getStatus()
                ? request.getStatus() : null;

        if (request.getAssignedAdminId() != null) {
            String assignee = StringUtils.trimToNull(request.getAssignedAdminId());
            if (assignee != null) {
                checkAssignee(assignee);
            }
            report.setAssignedAdminId(assignee);
        }
        if (newStatus != null) {
            report.setStatus(newStatus);
            report.setResolvedAt(newStatus == UserReportStatus.RESOLVED ? now : null);
        }
        report.setUpdatedAt(now);
        reportRepository.save(report);

        String note = StringUtils.trimToNull(request.getNote());
        if (newStatus != null || note != null) {
            UserReportNote entry = new UserReportNote();
            entry.setReportId(report.getId());
            entry.setAdminId(admin.getAdminId());
            entry.setStatus(newStatus);
            entry.setNote(note);
            entry.setCreatedAt(now);
            noteRepository.save(entry);
        }
        adminActivityService.createAdminActivity(admin.getAdminId(), null, AdminActivityEvent.UPDATE_USER_REPORT,
                "report " + report.getId() + (newStatus != null ? " -> " + newStatus : ""), null, null);
        if (newStatus == UserReportStatus.RESOLVED) {
            notifyResolved(report);
        }
        return detail(report, ErrorConstant.USER_REPORT_UPDATE_SUCCESS);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void notifyResolved(UserReport report) {
        try {
            Member member = validationService.getMemberById(report.getMemberId());
            notificationService.notify(member, NotificationType.SYSTEM, "USER_REPORT:" + report.getId() + ":RESOLVED",
                    NotificationMessages.reportResolved(member.getLanguage()),
                    Map.of("route", "report", "reportId", report.getId()));
        } catch (RuntimeException e) {
            log.warn("Report resolved notification failed. reportId: {} reason: {}",
                    report.getId(), e.getClass().getSimpleName());
        }
    }

    private void checkAssignee(String adminId) {
        boolean valid = adminRepository.findById(adminId)
                .filter(a -> a.getStatus() == ActiveStatus.ACTIVE)
                .filter(a -> RolePermissionMapper.hasPermission(a.getRole(), AdminPermission.REPORT_HANDLE))
                .isPresent();
        if (!valid) {
            throw new BadRequestException(ErrorConstant.USER_REPORT_ASSIGNEE_INVALID);
        }
    }

    /** null without attachment; rejects anything that is not a small image. */
    static byte[] decodeAttachment(String base64) {
        if (StringUtils.isBlank(base64)) {
            return null;
        }
        byte[] image;
        try {
            image = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorConstant.USER_REPORT_ATTACHMENT_INVALID);
        }
        if (image.length == 0 || image.length > AccountService.MAX_PHOTO_BYTES || AccountService.imageType(image) == null) {
            throw new BadRequestException(ErrorConstant.USER_REPORT_ATTACHMENT_INVALID);
        }
        return image;
    }

    private UserReport find(String reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.USER_REPORT_NOT_FOUND));
    }

    private BaseResponse<UserReportResponse> detail(UserReport report, ErrorConstant status) {
        List<UserReportNote> notes = noteRepository.findByReportIdOrderByCreatedAtAsc(report.getId());
        Map<String, String> admins = adminNames(Stream.concat(Stream.of(report.getAssignedAdminId()),
                notes.stream().map(UserReportNote::getAdminId)));
        Member member = memberRepository.findById(report.getMemberId()).orElse(null);
        return new BaseResponse<>(status.getCode(), status.getMessage(), toPortalResponse(report, member, admins, notes));
    }

    private Map<String, String> adminNames(Stream<String> ids) {
        List<String> distinct = ids.filter(Objects::nonNull).distinct().toList();
        // HashMap: looked up with a null assignee id
        return distinct.isEmpty() ? new HashMap<>() : adminRepository.findAllById(distinct).stream()
                .collect(Collectors.toMap(Admin::getId, Admin::getName, (a, b) -> a, HashMap::new));
    }

    static UserReportResponse toMemberResponse(UserReport report) {
        UserReportResponse response = new UserReportResponse();
        response.setId(report.getId());
        response.setCategory(report.getCategory());
        response.setDescription(report.getDescription());
        response.setHasAttachment(report.isHasAttachment());
        response.setStatus(report.getStatus());
        response.setCreatedAt(report.getCreatedAt());
        response.setUpdatedAt(report.getUpdatedAt());
        response.setResolvedAt(report.getResolvedAt());
        return response;
    }

    private static UserReportResponse toPortalResponse(UserReport report, Member member, Map<String, String> admins,
                                                       List<UserReportNote> notes) {
        UserReportResponse response = toMemberResponse(report);
        response.setMemberId(report.getMemberId());
        if (member != null) {
            response.setMemberName((member.getFirstName() + " " + member.getLastName()).trim());
            response.setMemberEmail(member.getEmail());
        }
        response.setAppVersion(report.getAppVersion());
        response.setPlatform(report.getPlatform());
        response.setAssignedAdminId(report.getAssignedAdminId());
        response.setAssignedAdminName(admins.get(report.getAssignedAdminId()));
        if (notes != null) {
            response.setHistory(notes.stream().map(n -> {
                UserReportResponse.Note note = new UserReportResponse.Note();
                note.setAdminName(admins.getOrDefault(n.getAdminId(), n.getAdminId()));
                note.setStatus(n.getStatus());
                note.setNote(n.getNote());
                note.setCreatedAt(n.getCreatedAt());
                return note;
            }).toList());
        }
        return response;
    }
}
