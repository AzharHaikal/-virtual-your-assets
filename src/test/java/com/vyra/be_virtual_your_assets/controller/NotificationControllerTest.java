package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationPreferencesResponse;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationResponse;
import com.vyra.be_virtual_your_assets.dto.notification.ReadAllResponse;
import com.vyra.be_virtual_your_assets.dto.notification.UnreadCountResponse;
import com.vyra.be_virtual_your_assets.dto.notification.UpdateNotificationPreferencesRequest;
import com.vyra.be_virtual_your_assets.dto.push.RegisterPushTokenRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.NotificationPreferenceService;
import com.vyra.be_virtual_your_assets.service.NotificationService;
import com.vyra.be_virtual_your_assets.service.PushTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;
    @Mock private NotificationPreferenceService preferenceService;
    @Mock private PushTokenService pushTokenService;

    @InjectMocks
    private NotificationController controller;

    @InjectMocks
    private PushTokenController pushTokenController;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(new CustomUserDetails("member-1", "token"), null);

    @Test
    void inbox_delegatesWithTheAuthenticatedMember() {
        BaseResponse<PageResponse<NotificationResponse>> list = new BaseResponse<>("VYRA-NTF-LST-000", "ok", null);
        when(notificationService.getNotifications("member-1", true, 1, 20)).thenReturn(list);
        assertThat(controller.getNotifications(true, 1, 20, authentication)).isSameAs(list);

        BaseResponse<UnreadCountResponse> count = new BaseResponse<>("VYRA-NTF-CNT-000", "ok", new UnreadCountResponse(2));
        when(notificationService.getUnreadCount("member-1")).thenReturn(count);
        assertThat(controller.getUnreadCount(authentication)).isSameAs(count);

        BaseResponse<NotificationResponse> read = new BaseResponse<>("VYRA-NTF-RD-000", "ok", null);
        when(notificationService.markRead("member-1", "n-1")).thenReturn(read);
        assertThat(controller.markRead("n-1", authentication)).isSameAs(read);

        BaseResponse<ReadAllResponse> all = new BaseResponse<>("VYRA-NTF-RDA-000", "ok", new ReadAllResponse(3));
        when(notificationService.markAllRead("member-1")).thenReturn(all);
        assertThat(controller.markAllRead(authentication)).isSameAs(all);
    }

    @Test
    void preferences_delegateWithTheAuthenticatedMember() {
        BaseResponse<NotificationPreferencesResponse> current = new BaseResponse<>("VYRA-NTF-PRF-000", "ok", null);
        when(preferenceService.getPreferences("member-1")).thenReturn(current);
        assertThat(controller.getPreferences(authentication)).isSameAs(current);

        UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest();
        BaseResponse<NotificationPreferencesResponse> saved = new BaseResponse<>("VYRA-NTF-PRF-001", "ok", null);
        when(preferenceService.updatePreferences("member-1", request)).thenReturn(saved);
        assertThat(controller.updatePreferences(request, authentication)).isSameAs(saved);
    }

    @Test
    void pushTokens_delegateWithTheAuthenticatedMember() {
        RegisterPushTokenRequest request = new RegisterPushTokenRequest();
        BaseResponse<Void> saved = new BaseResponse<>("VYRA-PSH-000", "ok", null);
        when(pushTokenService.register("member-1", request)).thenReturn(saved);
        assertThat(pushTokenController.register(request, authentication)).isSameAs(saved);

        BaseResponse<Void> deleted = new BaseResponse<>("VYRA-PSH-DEL-000", "ok", null);
        when(pushTokenService.unregister("member-1", "fcm-token")).thenReturn(deleted);
        assertThat(pushTokenController.unregister("fcm-token", authentication)).isSameAs(deleted);
    }
}
