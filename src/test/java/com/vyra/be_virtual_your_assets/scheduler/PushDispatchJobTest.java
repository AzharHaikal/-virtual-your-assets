package com.vyra.be_virtual_your_assets.scheduler;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.DeliveryStatus;
import com.vyra.be_virtual_your_assets.constant.NotificationChannel;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.entity.DevicePushToken;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Notification;
import com.vyra.be_virtual_your_assets.entity.NotificationDelivery;
import com.vyra.be_virtual_your_assets.repository.DevicePushTokenRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.NotificationDeliveryRepository;
import com.vyra.be_virtual_your_assets.repository.NotificationRepository;
import com.vyra.be_virtual_your_assets.service.NotificationPreferenceService;
import com.vyra.be_virtual_your_assets.service.NotificationRelevance;
import com.vyra.be_virtual_your_assets.service.PushClient;
import com.vyra.be_virtual_your_assets.service.TimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushDispatchJobTest {

    /** 05:00 UTC = 12:00 WIB. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 22, 5, 0);

    @Mock private NotificationDeliveryRepository deliveryRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private DevicePushTokenRepository tokenRepository;
    @Mock private NotificationPreferenceService preferenceService;
    @Mock private NotificationRelevance relevance;
    @Mock private PushClient pushClient;

    private PushDispatchJob job;
    private Member member;
    private Notification notification;
    private NotificationDelivery delivery;

    @BeforeEach
    void setUp() {
        job = new PushDispatchJob(deliveryRepository, notificationRepository, memberRepository, tokenRepository,
                preferenceService, relevance, pushClient, new TimeService(Clock.systemUTC(), "Asia/Jakarta"));
        member = new Member();
        member.setId("m-1");
        member.setTimezone("Asia/Jakarta");
        member.setStatus(ActiveStatus.ACTIVE);
        notification = new Notification();
        notification.setId("n-1");
        notification.setMemberId("m-1");
        notification.setType(NotificationType.HABIT_REMINDER);
        notification.setTitle("Waktunya Minum air");
        notification.setBody("...");
        notification.setPayload(Map.of("route", "habit", "habitId", "h-1"));
        delivery = new NotificationDelivery();
        delivery.setId("d-1");
        delivery.setNotificationId("n-1");
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setDeliverAfter(NOW.minusMinutes(1));

        lenient().when(notificationRepository.findById("n-1")).thenReturn(Optional.of(notification));
        lenient().when(memberRepository.findById("m-1")).thenReturn(Optional.of(member));
        lenient().when(pushClient.isEnabled()).thenReturn(true);
        lenient().when(preferenceService.resolve("m-1")).thenReturn(preferences(true, null, null));
        lenient().when(relevance.isRelevant(notification, member)).thenReturn(true);
    }

    @Test
    void sendsToEveryDevice_forgetsInvalidOnes_andMarksSent() {
        DevicePushToken phone = device("token-phone");
        DevicePushToken old = device("token-old");
        when(tokenRepository.findByMemberId("m-1")).thenReturn(List.of(phone, old));
        when(pushClient.send(eq("token-phone"), any(), any(), anyMap())).thenReturn(PushClient.Result.SENT);
        when(pushClient.send(eq("token-old"), any(), any(), anyMap())).thenReturn(PushClient.Result.INVALID_TOKEN);

        job.dispatch(delivery, NOW);

        verify(tokenRepository).delete(old);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(delivery.getSentAt()).isEqualTo(NOW);
        assertThat(PushDispatchJob.data(notification)).containsEntry("notificationId", "n-1").containsEntry("habitId", "h-1");
    }

    @Test
    void quietHoursSetSinceThen_postponeTheDelivery() {
        // 12:00 WIB inside 11:00-13:00 quiet hours -> 13:00 WIB = 06:00 UTC.
        when(preferenceService.resolve("m-1")).thenReturn(preferences(true, LocalTime.of(11, 0), LocalTime.of(13, 0)));

        job.dispatch(delivery, NOW);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(delivery.getDeliverAfter()).isEqualTo(LocalDateTime.of(2026, 10, 22, 6, 0));
        verify(pushClient, never()).send(any(), any(), any(), any());
    }

    @Test
    void noLongerRelevant_pushOff_orInactiveMember_isSkipped() {
        when(relevance.isRelevant(notification, member)).thenReturn(false);
        job.dispatch(delivery, NOW);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SKIPPED);

        delivery.setStatus(DeliveryStatus.PENDING);
        when(preferenceService.resolve("m-1")).thenReturn(preferences(false, null, null));
        job.dispatch(delivery, NOW);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SKIPPED);

        delivery.setStatus(DeliveryStatus.PENDING);
        member.setStatus(ActiveStatus.DELETED);
        job.dispatch(delivery, NOW);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SKIPPED);
        verify(pushClient, never()).send(any(), any(), any(), any());
    }

    @Test
    void aTemporaryFailure_isRetriedAFewTimes_thenFails() {
        when(tokenRepository.findByMemberId("m-1")).thenReturn(List.of(device("token-phone")));
        when(pushClient.send(any(), any(), any(), anyMap())).thenReturn(PushClient.Result.FAILED);

        job.dispatch(delivery, NOW);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(delivery.getAttempts()).isEqualTo(1);
        assertThat(delivery.getDeliverAfter()).isEqualTo(NOW.plusMinutes(PushDispatchJob.RETRY_MINUTES));

        job.dispatch(delivery, NOW);
        job.dispatch(delivery, NOW);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
    }

    @Test
    void run_handlesTheDueDeliveries_andOneFailureDoesNotStopTheOthers() {
        NotificationDelivery broken = new NotificationDelivery();
        broken.setId("d-0");
        broken.setNotificationId("n-broken");
        when(notificationRepository.findById("n-broken")).thenThrow(new IllegalStateException("db"));
        when(deliveryRepository.findDue(eq(DeliveryStatus.PENDING), eq(NOW), any())).thenReturn(List.of(broken, delivery));
        when(tokenRepository.findByMemberId("m-1")).thenReturn(List.of());

        assertThat(job.run(NOW)).isEqualTo(2);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SKIPPED); // no device
    }

    private static DevicePushToken device(String token) {
        DevicePushToken device = new DevicePushToken();
        device.setId("dev-" + token);
        device.setMemberId("m-1");
        device.setToken(token);
        return device;
    }

    private static NotificationPreferenceService.Preferences preferences(boolean push, LocalTime start, LocalTime end) {
        Map<NotificationType, Map<NotificationChannel, Boolean>> enabled = new EnumMap<>(NotificationType.class);
        for (NotificationType type : NotificationType.values()) {
            Map<NotificationChannel, Boolean> channels = new EnumMap<>(NotificationChannel.class);
            channels.put(NotificationChannel.IN_APP, true);
            channels.put(NotificationChannel.PUSH, push);
            channels.put(NotificationChannel.EMAIL, false);
            enabled.put(type, channels);
        }
        return new NotificationPreferenceService.Preferences(enabled, start, end);
    }
}
