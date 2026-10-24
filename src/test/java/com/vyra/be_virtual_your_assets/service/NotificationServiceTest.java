package com.vyra.be_virtual_your_assets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.DeliveryStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.NotificationChannel;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationResponse;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Notification;
import com.vyra.be_virtual_your_assets.entity.NotificationDelivery;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.NotificationDeliveryRepository;
import com.vyra.be_virtual_your_assets.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final String MEMBER = "member-1";
    /** 2026-10-22 16:30 UTC = 23:30 in Jakarta. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 22, 16, 30);

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationDeliveryRepository deliveryRepository;
    @Mock private NotificationPreferenceService preferenceService;

    private NotificationService service;
    private Member member;

    @BeforeEach
    void setUp() {
        TimeService timeService = new TimeService(Clock.fixed(Instant.parse("2026-10-22T16:30:00Z"), ZoneOffset.UTC),
                "Asia/Jakarta");
        service = new NotificationService(notificationRepository, deliveryRepository, preferenceService, timeService,
                new ObjectMapper());
        member = new Member();
        member.setId(MEMBER);
        member.setTimezone("Asia/Jakarta");
        member.setLanguage(AppLanguage.ID);
    }

    @Test
    void notify_createsTheInboxRow_andAPushDelivery_heldUntilQuietHoursEnd() {
        when(preferenceService.resolve(MEMBER)).thenReturn(preferences(true, true, LocalTime.of(22, 0), LocalTime.of(7, 0)));
        when(notificationRepository.insertIfAbsent(anyString(), eq(MEMBER), eq("HABIT_REMINDER"), anyString(), anyString(),
                eq("{\"route\":\"habit\"}"), eq("HABIT:h-1:2026-10-22"), isNull(), eq(NOW))).thenReturn(1);

        boolean created = service.notify(member, NotificationType.HABIT_REMINDER, "HABIT:h-1:2026-10-22",
                NotificationMessages.habitReminder(AppLanguage.ID, "Minum air"), Map.of("route", "habit"));

        assertThat(created).isTrue();
        ArgumentCaptor<NotificationDelivery> delivery = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(deliveryRepository).save(delivery.capture());
        assertThat(delivery.getValue().getChannel()).isEqualTo(NotificationChannel.PUSH);
        assertThat(delivery.getValue().getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(delivery.getValue().getDeliverAfter()).isEqualTo(LocalDateTime.of(2026, 10, 23, 0, 0)); // 07:00 WIB
    }

    @Test
    void notify_sameDedupKeyAgain_createsNothing() {
        when(preferenceService.resolve(MEMBER)).thenReturn(preferences(true, true, null, null));
        when(notificationRepository.insertIfAbsent(anyString(), anyString(), anyString(), anyString(), anyString(), any(),
                anyString(), any(), any())).thenReturn(0);

        assertThat(service.notify(member, NotificationType.GOAL_DUE, "GOAL_DUE:g-1:2026-10-25",
                NotificationMessages.goalDue(AppLanguage.EN, "Laptop", java.time.LocalDate.of(2026, 10, 25)), null)).isFalse();
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void notify_typeTurnedOffEverywhere_isNotCreated_andInAppOff_isCreatedAsRead() {
        when(preferenceService.resolve(MEMBER)).thenReturn(preferences(false, false, null, null));
        assertThat(service.notify(member, NotificationType.BUDGET_ALERT, "BUDGET:b-1:80",
                NotificationMessages.budgetAlert(AppLanguage.EN, "Food", 85), null)).isFalse();
        verify(notificationRepository, never()).insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any(), any());

        when(preferenceService.resolve(MEMBER)).thenReturn(preferences(false, true, null, null));
        when(notificationRepository.insertIfAbsent(anyString(), anyString(), anyString(), anyString(), anyString(), any(),
                anyString(), eq(NOW), eq(NOW))).thenReturn(1);
        assertThat(service.notify(member, NotificationType.BUDGET_ALERT, "BUDGET:b-1:80",
                NotificationMessages.budgetAlert(AppLanguage.EN, "Food", 85), null)).isTrue();
    }

    @Test
    void inbox_listCountAndRead_onlyTheOwnNotifications() {
        Notification unread = notification("n-1");
        when(notificationRepository.findByMemberIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(eq(MEMBER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unread)));
        PageResponse<NotificationResponse> page = service.getNotifications(MEMBER, true, 0, 500).getData();
        assertThat(page.getContent()).extracting(NotificationResponse::getId).containsExactly("n-1");

        when(notificationRepository.countByMemberIdAndReadAtIsNull(MEMBER)).thenReturn(3L);
        assertThat(service.getUnreadCount(MEMBER).getData().getCount()).isEqualTo(3);

        when(notificationRepository.findByIdAndMemberId("n-1", MEMBER)).thenReturn(Optional.of(unread));
        assertThat(service.markRead(MEMBER, "n-1").getData().getReadAt()).isEqualTo(NOW);
        // Reading again keeps the first time.
        LocalDateTime first = unread.getReadAt();
        assertThat(service.markRead(MEMBER, "n-1").getData().getReadAt()).isEqualTo(first);

        NotFoundException foreign = assertThrows(NotFoundException.class, () -> service.markRead(MEMBER, "n-other"));
        assertThat(foreign.getErrorConstant()).isEqualTo(ErrorConstant.NOTIFICATION_NOT_FOUND);

        when(notificationRepository.markAllRead(MEMBER, NOW)).thenReturn(4);
        assertThat(service.markAllRead(MEMBER).getData().getUpdated()).isEqualTo(4);
    }

    @Test
    void messages_areInTheMembersLanguage_andCutToTheColumnSize() {
        assertThat(NotificationMessages.habitReminder(AppLanguage.ID, "Minum air").title()).isEqualTo("Waktunya Minum air");
        assertThat(NotificationMessages.budgetAlert(AppLanguage.EN, "Food", 100).title()).isEqualTo("Food budget used up");
        assertThat(NotificationMessages.habitReminder(AppLanguage.EN, "x".repeat(300)).title())
                .hasSize(NotificationMessages.MAX_TITLE);
    }

    private static NotificationPreferenceService.Preferences preferences(boolean inApp, boolean push,
                                                                         LocalTime quietStart, LocalTime quietEnd) {
        Map<NotificationType, Map<NotificationChannel, Boolean>> enabled = new EnumMap<>(NotificationType.class);
        for (NotificationType type : NotificationType.values()) {
            Map<NotificationChannel, Boolean> channels = new EnumMap<>(NotificationChannel.class);
            channels.put(NotificationChannel.IN_APP, inApp);
            channels.put(NotificationChannel.PUSH, push);
            channels.put(NotificationChannel.EMAIL, false);
            enabled.put(type, channels);
        }
        return new NotificationPreferenceService.Preferences(enabled, quietStart, quietEnd);
    }

    private static Notification notification(String id) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setMemberId(MEMBER);
        notification.setType(NotificationType.HABIT_REMINDER);
        notification.setTitle("t");
        notification.setBody("b");
        notification.setDedupKey("k-" + id);
        notification.setCreatedAt(NOW.minusHours(1));
        return notification;
    }
}
