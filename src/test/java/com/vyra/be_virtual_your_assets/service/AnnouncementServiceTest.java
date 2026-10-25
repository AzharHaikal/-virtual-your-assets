package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.constant.AnnouncementStatus;
import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.dto.announcement.AnnouncementRequest;
import com.vyra.be_virtual_your_assets.dto.announcement.AnnouncementResponse;
import com.vyra.be_virtual_your_assets.entity.Announcement;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.AnnouncementRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    /** 2026-10-25 03:00 UTC = 10:00 WIB. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 25, 3, 0);

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private NotificationService notificationService;
    @Mock private ValidationService validationService;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @Mock private AdminActivityService adminActivityService;

    private AnnouncementService service;
    private final CustomAdminDetails admin = new CustomAdminDetails("admin-1", "token", AdminRole.ADMINISTRATOR);

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(announcementRepository, memberRepository, notificationService,
                validationService, adminAuthorizationService, adminActivityService,
                new TimeService(Clock.fixed(Instant.parse("2026-10-25T03:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
    }

    @Test
    void create_savesDraft_readingTimesWithoutOffsetInWib() {
        AnnouncementResponse result = service.create(request("2026-10-26T08:00:00", "2026-10-27T08:00:00+07:00"), admin)
                .getData();

        verify(adminAuthorizationService).requirePermission(AdminPermission.ANNOUNCEMENT_MANAGE);
        ArgumentCaptor<Announcement> saved = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(AnnouncementStatus.DRAFT);
        assertThat(saved.getValue().getStartsAt()).isEqualTo(LocalDateTime.of(2026, 10, 26, 1, 0));
        assertThat(saved.getValue().getEndsAt()).isEqualTo(LocalDateTime.of(2026, 10, 27, 1, 0));
        assertThat(saved.getValue().getTitle()).isEqualTo("Fitur baru");
        assertThat(result.isActive()).isFalse();
        verify(adminActivityService).createAdminActivity(eq("admin-1"), isNull(),
                eq(AdminActivityEvent.CREATE_ANNOUNCEMENT), eq("Fitur baru"), isNull(), isNull());
    }

    @Test
    void create_endBeforeStart_isRejected() {
        assertThrows(BadRequestException.class,
                () -> service.create(request("2026-10-26T08:00:00", "2026-10-26T07:00:00"), admin));
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void publish_thenUnpublish_changesStatusOnceEach() {
        Announcement announcement = announcement("a-1", "ALL");
        announcement.setStatus(AnnouncementStatus.DRAFT);
        when(announcementRepository.findByIdAndDeletedAtIsNull("a-1")).thenReturn(Optional.of(announcement));

        assertThat(service.publish("a-1", admin).getData().isActive()).isTrue();
        service.publish("a-1", admin);
        assertThat(announcement.getPublishedAt()).isEqualTo(NOW);
        service.unpublish("a-1", admin);

        assertThat(announcement.getStatus()).isEqualTo(AnnouncementStatus.UNPUBLISHED);
        verify(adminActivityService, times(1)).createAdminActivity(eq("admin-1"), isNull(),
                eq(AdminActivityEvent.PUBLISH_ANNOUNCEMENT), anyString(), isNull(), isNull());
        verify(adminActivityService, times(1)).createAdminActivity(eq("admin-1"), isNull(),
                eq(AdminActivityEvent.UNPUBLISH_ANNOUNCEMENT), anyString(), isNull(), isNull());
    }

    @Test
    void delete_unknown_isNotFound() {
        when(announcementRepository.findByIdAndDeletedAtIsNull("a-x")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete("a-x", admin));
    }

    @Test
    void getActive_usesTheMembersLanguage() {
        Member member = member("m-1", AppLanguage.EN);
        when(validationService.getMemberById("m-1")).thenReturn(member);
        when(announcementRepository.findActive(NOW, "EN")).thenReturn(List.of(announcement("a-1", "EN")));

        List<AnnouncementResponse> result = service.getActive("m-1").getData();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeliveredAt()).isNull();
    }

    @Test
    void deliverDue_notifiesActiveMembersOfTheLanguageOnce() {
        Announcement announcement = announcement("a-1", "ID");
        when(announcementRepository.findToDeliver(NOW)).thenReturn(List.of(announcement));
        Member indonesian = member("m-1", AppLanguage.ID);
        Member english = member("m-2", AppLanguage.EN);
        Pageable first = PageRequest.of(0, AnnouncementService.DELIVERY_BATCH);
        when(memberRepository.findByStatusOrderByIdAsc(ActiveStatus.ACTIVE, first))
                .thenReturn(new PageImpl<>(List.of(indonesian, english), first, 2));
        when(notificationService.notify(eq(indonesian), eq(NotificationType.ANNOUNCEMENT), eq("ANNOUNCEMENT:a-1:m-1"),
                any(), eq(Map.of("route", "announcement", "announcementId", "a-1")))).thenReturn(true);

        assertThat(service.deliverDue(NOW)).isEqualTo(1);

        verify(notificationService, never()).notify(eq(english), any(), anyString(), any(), any());
        assertThat(announcement.getDeliveredAt()).isEqualTo(NOW);
        verify(announcementRepository).save(announcement);
    }

    private static AnnouncementRequest request(String startsAt, String endsAt) {
        AnnouncementRequest request = new AnnouncementRequest();
        request.setTitle(" Fitur baru ");
        request.setBody("Coba analitik lanjutan.");
        request.setStartsAt(startsAt);
        request.setEndsAt(endsAt);
        return request;
    }

    private static Announcement announcement(String id, String language) {
        Announcement announcement = new Announcement();
        announcement.setId(id);
        announcement.setTitle("Fitur baru");
        announcement.setBody("Coba analitik lanjutan.");
        announcement.setLanguage(language);
        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setStartsAt(NOW.minusHours(1));
        return announcement;
    }

    private static Member member(String id, AppLanguage language) {
        Member member = new Member();
        member.setId(id);
        member.setLanguage(language);
        return member;
    }
}
