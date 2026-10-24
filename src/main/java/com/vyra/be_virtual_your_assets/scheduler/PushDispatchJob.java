package com.vyra.be_virtual_your_assets.scheduler;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.DeliveryStatus;
import com.vyra.be_virtual_your_assets.constant.NotificationChannel;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends the push deliveries whose time has come. Before sending, the member's current choices are
 * checked again (push still on, quiet hours possibly changed) and whether the notification is still
 * relevant. Devices FCM reports as gone are forgotten; a temporary failure is retried a few times.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PushDispatchJob {

    static final int BATCH_SIZE = 200;
    static final int MAX_ATTEMPTS = 3;
    static final int RETRY_MINUTES = 5;

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final DevicePushTokenRepository tokenRepository;
    private final NotificationPreferenceService preferenceService;
    private final NotificationRelevance relevance;
    private final PushClient pushClient;
    private final TimeService timeService;

    @Scheduled(cron = "30 * * * * *")
    @SchedulerLock(name = "pushDispatchJob", lockAtMostFor = "PT5M")
    public void scheduled() {
        run(timeService.nowUtc());
    }

    /** @return deliveries handled (sent, skipped, rescheduled or failed) */
    public int run(LocalDateTime nowUtc) {
        List<NotificationDelivery> due = deliveryRepository.findDue(DeliveryStatus.PENDING, nowUtc,
                PageRequest.of(0, BATCH_SIZE));
        for (NotificationDelivery delivery : due) {
            try {
                dispatch(delivery, nowUtc);
            } catch (RuntimeException e) {
                log.warn("Push delivery failed. deliveryId: {} reason: {}", delivery.getId(), e.getClass().getSimpleName());
            }
        }
        return due.size();
    }

    void dispatch(NotificationDelivery delivery, LocalDateTime now) {
        Notification notification = notificationRepository.findById(delivery.getNotificationId()).orElse(null);
        Member member = notification == null ? null : memberRepository.findById(notification.getMemberId()).orElse(null);
        if (member == null || member.getStatus() != ActiveStatus.ACTIVE || !pushClient.isEnabled()) {
            finish(delivery, DeliveryStatus.SKIPPED, null);
            return;
        }
        NotificationPreferenceService.Preferences preferences = preferenceService.resolve(member.getId());
        if (!preferences.isEnabled(notification.getType(), NotificationChannel.PUSH)) {
            finish(delivery, DeliveryStatus.SKIPPED, null);
            return;
        }
        LocalDateTime allowedFrom = NotificationPreferenceService.deliverAfter(preferences, timeService.zoneOf(member), now);
        if (allowedFrom.isAfter(now)) {
            // Quiet hours set or moved since the notification was created.
            delivery.setDeliverAfter(allowedFrom);
            deliveryRepository.save(delivery);
            return;
        }
        if (!relevance.isRelevant(notification, member)) {
            finish(delivery, DeliveryStatus.SKIPPED, null);
            return;
        }
        List<DevicePushToken> devices = tokenRepository.findByMemberId(member.getId());
        boolean sent = false;
        boolean failed = false;
        for (DevicePushToken device : devices) {
            switch (pushClient.send(device.getToken(), notification.getTitle(), notification.getBody(), data(notification))) {
                case SENT -> sent = true;
                case INVALID_TOKEN -> tokenRepository.delete(device);
                case FAILED -> failed = true;
            }
        }
        if (sent) {
            finish(delivery, DeliveryStatus.SENT, now);
        } else if (failed) {
            retry(delivery, now);
        } else {
            finish(delivery, DeliveryStatus.SKIPPED, null); // no (valid) device
        }
    }

    private void retry(NotificationDelivery delivery, LocalDateTime now) {
        delivery.setAttempts(delivery.getAttempts() + 1);
        if (delivery.getAttempts() >= MAX_ATTEMPTS) {
            finish(delivery, DeliveryStatus.FAILED, null);
            return;
        }
        delivery.setDeliverAfter(now.plusMinutes((long) RETRY_MINUTES * delivery.getAttempts()));
        deliveryRepository.save(delivery);
    }

    private void finish(NotificationDelivery delivery, DeliveryStatus status, LocalDateTime sentAt) {
        delivery.setStatus(status);
        delivery.setSentAt(sentAt);
        deliveryRepository.save(delivery);
    }

    /** The deep link plus the notification id, so a tap can mark it read. */
    static Map<String, String> data(Notification notification) {
        Map<String, String> data = new HashMap<>();
        if (notification.getPayload() != null) {
            data.putAll(notification.getPayload());
        }
        data.put("notificationId", notification.getId());
        data.put("type", notification.getType().name());
        return data;
    }
}
