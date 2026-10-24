package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationPreferencesResponse;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationResponse;
import com.vyra.be_virtual_your_assets.dto.notification.ReadAllResponse;
import com.vyra.be_virtual_your_assets.dto.notification.UnreadCountResponse;
import com.vyra.be_virtual_your_assets.dto.notification.UpdateNotificationPreferencesRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.NotificationPreferenceService;
import com.vyra.be_virtual_your_assets.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The member's notification inbox and notification settings (no admin access). */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_NOTIFICATIONS)
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    /** Newest first. */
    @GetMapping
    public BaseResponse<PageResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return notificationService.getNotifications(memberId(authentication), unreadOnly, page, size);
    }

    /** For the badge on the bell. */
    @GetMapping(ApiPath.NOTIFICATION_UNREAD_COUNT)
    public BaseResponse<UnreadCountResponse> getUnreadCount(Authentication authentication) {
        return notificationService.getUnreadCount(memberId(authentication));
    }

    @PostMapping(ApiPath.NOTIFICATION_READ)
    public BaseResponse<NotificationResponse> markRead(@PathVariable String notificationId, Authentication authentication) {
        return notificationService.markRead(memberId(authentication), notificationId);
    }

    @PostMapping(ApiPath.NOTIFICATION_READ_ALL)
    public BaseResponse<ReadAllResponse> markAllRead(Authentication authentication) {
        return notificationService.markAllRead(memberId(authentication));
    }

    @GetMapping(ApiPath.NOTIFICATION_PREFERENCES)
    public BaseResponse<NotificationPreferencesResponse> getPreferences(Authentication authentication) {
        return preferenceService.getPreferences(memberId(authentication));
    }

    @PutMapping(ApiPath.NOTIFICATION_PREFERENCES)
    public BaseResponse<NotificationPreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdateNotificationPreferencesRequest request,
            Authentication authentication
    ) {
        return preferenceService.updatePreferences(memberId(authentication), request);
    }

    private static String memberId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
