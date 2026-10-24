package com.vyra.be_virtual_your_assets.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyra.be_virtual_your_assets.constant.DeliveryStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.NotificationChannel;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationResponse;
import com.vyra.be_virtual_your_assets.dto.notification.ReadAllResponse;
import com.vyra.be_virtual_your_assets.dto.notification.UnreadCountResponse;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Notification;
import com.vyra.be_virtual_your_assets.entity.NotificationDelivery;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.NotificationDeliveryRepository;
import com.vyra.be_virtual_your_assets.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * The member's notification inbox, and the single way the server creates a notification. The inbox row
 * is the source of truth; a push delivery is added when the member wants push for that type, timed after
 * their quiet hours. Titles and bodies can contain names of habits / goals: they are never logged.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationPreferenceService preferenceService;
    private final TimeService timeService;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // Create (used by the reminder / alert sources)
    // =========================================================================

    /**
     * Creates the notification unless one with {@code dedupKey} already exists or the member turned this
     * type off on every channel. With IN_APP off (but PUSH on) it is created as already read.
     *
     * @return true when it was created now
     */
    @Transactional
    public boolean notify(Member member, NotificationType type, String dedupKey, NotificationMessages.Message message,
                          Map<String, String> payload) {
        NotificationPreferenceService.Preferences preferences = preferenceService.resolve(member.getId());
        boolean inApp = preferences.isEnabled(type, NotificationChannel.IN_APP);
        boolean push = preferences.isEnabled(type, NotificationChannel.PUSH);
        if (!inApp && !push) {
            return false;
        }
        LocalDateTime now = timeService.nowUtc();
        String id = UUID.randomUUID().toString();
        int created = notificationRepository.insertIfAbsent(id, member.getId(), type.name(), message.title(),
                message.body(), toJson(payload), dedupKey, inApp ? null : now, now);
        if (created == 0) {
            return false;
        }
        if (push) {
            NotificationDelivery delivery = new NotificationDelivery();
            delivery.setNotificationId(id);
            delivery.setChannel(NotificationChannel.PUSH);
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setDeliverAfter(NotificationPreferenceService.deliverAfter(preferences, timeService.zoneOf(member), now));
            delivery.setCreatedAt(now);
            deliveryRepository.save(delivery);
        }
        log.info("Notification created. memberId: {} type: {} push: {}", member.getId(), type, push);
        return true;
    }

    // =========================================================================
    // Inbox
    // =========================================================================

    @Transactional(readOnly = true)
    public BaseResponse<PageResponse<NotificationResponse>> getNotifications(String memberId, boolean unreadOnly,
                                                                             int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<Notification> notifications = unreadOnly
                ? notificationRepository.findByMemberIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(memberId, pageable)
                : notificationRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId, pageable);
        return new BaseResponse<>(ErrorConstant.NOTIFICATION_LIST_SUCCESS.getCode(),
                ErrorConstant.NOTIFICATION_LIST_SUCCESS.getMessage(),
                PageResponse.from(notifications.map(NotificationService::toResponse)));
    }

    @Transactional(readOnly = true)
    public BaseResponse<UnreadCountResponse> getUnreadCount(String memberId) {
        return new BaseResponse<>(ErrorConstant.NOTIFICATION_UNREAD_COUNT_SUCCESS.getCode(),
                ErrorConstant.NOTIFICATION_UNREAD_COUNT_SUCCESS.getMessage(),
                new UnreadCountResponse(notificationRepository.countByMemberIdAndReadAtIsNull(memberId)));
    }

    /** Idempotent: a read notification keeps its first read time. */
    @Transactional
    public BaseResponse<NotificationResponse> markRead(String memberId, String notificationId) {
        Notification notification = notificationRepository.findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(() -> new NotFoundException(ErrorConstant.NOTIFICATION_NOT_FOUND));
        if (notification.getReadAt() == null) {
            notification.setReadAt(timeService.nowUtc());
            notificationRepository.save(notification);
        }
        return new BaseResponse<>(ErrorConstant.NOTIFICATION_READ_SUCCESS.getCode(),
                ErrorConstant.NOTIFICATION_READ_SUCCESS.getMessage(), toResponse(notification));
    }

    @Transactional
    public BaseResponse<ReadAllResponse> markAllRead(String memberId) {
        int updated = notificationRepository.markAllRead(memberId, timeService.nowUtc());
        return new BaseResponse<>(ErrorConstant.NOTIFICATION_READ_ALL_SUCCESS.getCode(),
                ErrorConstant.NOTIFICATION_READ_ALL_SUCCESS.getMessage(), new ReadAllResponse(updated));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String toJson(Map<String, String> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Notification payload is not serialisable", e);
        }
    }

    static NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setBody(notification.getBody());
        response.setPayload(notification.getPayload());
        response.setReadAt(notification.getReadAt());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}
