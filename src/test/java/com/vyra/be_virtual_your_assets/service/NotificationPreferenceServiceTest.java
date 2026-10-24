package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.NotificationChannel;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationPreferenceItem;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationPreferencesResponse;
import com.vyra.be_virtual_your_assets.dto.notification.UpdateNotificationPreferencesRequest;
import com.vyra.be_virtual_your_assets.entity.NotificationPreference;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    private static final String MEMBER = "member-1";
    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

    @Mock private NotificationPreferenceRepository repository;

    private NotificationPreferenceService service;

    @BeforeEach
    void setUp() {
        service = new NotificationPreferenceService(repository,
                new TimeService(Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta"));
    }

    @Test
    void resolve_withoutRows_inAppAndPushOn_emailOff_noQuietHours() {
        NotificationPreferenceService.Preferences preferences = service.resolve(MEMBER);

        assertThat(preferences.isEnabled(NotificationType.HABIT_REMINDER, NotificationChannel.IN_APP)).isTrue();
        assertThat(preferences.isEnabled(NotificationType.BUDGET_ALERT, NotificationChannel.PUSH)).isTrue();
        assertThat(preferences.isEnabled(NotificationType.GOAL_DUE, NotificationChannel.EMAIL)).isFalse();
        assertThat(preferences.hasQuietHours()).isFalse();
    }

    @Test
    void resolve_storedRowsOverrideTheDefaults() {
        when(repository.findByMemberId(MEMBER)).thenReturn(List.of(
                row(NotificationType.HABIT_REMINDER, NotificationChannel.PUSH, false, LocalTime.of(22, 0), LocalTime.of(7, 0))));

        NotificationPreferenceService.Preferences preferences = service.resolve(MEMBER);

        assertThat(preferences.isEnabled(NotificationType.HABIT_REMINDER, NotificationChannel.PUSH)).isFalse();
        assertThat(preferences.isEnabled(NotificationType.HABIT_REMINDER, NotificationChannel.IN_APP)).isTrue();
        assertThat(preferences.quietStart()).isEqualTo(LocalTime.of(22, 0));
        assertThat(preferences.quietEnd()).isEqualTo(LocalTime.of(7, 0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void update_mergesTheItems_andWritesEveryCombinationWithTheQuietHours() {
        UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest();
        request.setQuietStart(LocalTime.of(22, 0, 30));
        request.setQuietEnd(LocalTime.of(6, 30));
        request.setItems(List.of(new NotificationPreferenceItem(NotificationType.GOAL_DUE, NotificationChannel.PUSH, false)));

        NotificationPreferencesResponse result = service.updatePreferences(MEMBER, request).getData();

        ArgumentCaptor<List<NotificationPreference>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        int combinations = NotificationType.values().length * NotificationChannel.values().length;
        assertThat(saved.getValue()).hasSize(combinations)
                .allSatisfy(r -> assertThat(r.getQuietStart()).isEqualTo(LocalTime.of(22, 0)));
        assertThat(result.getItems()).hasSize(combinations)
                .contains(new NotificationPreferenceItem(NotificationType.GOAL_DUE, NotificationChannel.PUSH, false))
                .contains(new NotificationPreferenceItem(NotificationType.GOAL_DUE, NotificationChannel.IN_APP, true));
        assertThat(result.getQuietEnd()).isEqualTo(LocalTime.of(6, 30));
    }

    @Test
    void update_onlyOneEndOfQuietHours_orBothEqual_isRejected() {
        UpdateNotificationPreferencesRequest half = new UpdateNotificationPreferencesRequest();
        half.setQuietStart(LocalTime.of(22, 0));
        BadRequestException missing = assertThrows(BadRequestException.class, () -> service.updatePreferences(MEMBER, half));
        assertThat(missing.getErrorConstant()).isEqualTo(ErrorConstant.NOTIFICATION_QUIET_HOURS_INVALID);

        UpdateNotificationPreferencesRequest same = new UpdateNotificationPreferencesRequest();
        same.setQuietStart(LocalTime.of(8, 0));
        same.setQuietEnd(LocalTime.of(8, 0));
        assertThrows(BadRequestException.class, () -> service.updatePreferences(MEMBER, same));
        verify(repository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void isQuiet_handlesWindowsThatPassMidnight() {
        LocalTime start = LocalTime.of(22, 0);
        LocalTime end = LocalTime.of(7, 0);
        assertThat(NotificationPreferenceService.isQuiet(LocalTime.of(23, 30), start, end)).isTrue();
        assertThat(NotificationPreferenceService.isQuiet(LocalTime.of(3, 0), start, end)).isTrue();
        assertThat(NotificationPreferenceService.isQuiet(LocalTime.of(7, 0), start, end)).isFalse();
        assertThat(NotificationPreferenceService.isQuiet(LocalTime.of(12, 0), start, end)).isFalse();
        assertThat(NotificationPreferenceService.isQuiet(LocalTime.of(13, 30), LocalTime.of(13, 0), LocalTime.of(15, 0))).isTrue();
    }

    @Test
    void deliverAfter_inQuietHours_isTheirEndInTheMembersZone_otherwiseNow() {
        NotificationPreferenceService.Preferences quiet = new NotificationPreferenceService.Preferences(
                service.resolve(MEMBER).enabled(), LocalTime.of(22, 0), LocalTime.of(7, 0));

        // 23:30 in Jakarta -> 07:00 the next morning (00:00 UTC).
        LocalDateTime lateEvening = LocalDateTime.of(2026, 10, 22, 16, 30);
        assertThat(NotificationPreferenceService.deliverAfter(quiet, JAKARTA, lateEvening))
                .isEqualTo(LocalDateTime.of(2026, 10, 23, 0, 0));
        // 03:00 in Jakarta -> 07:00 the same morning.
        LocalDateTime night = LocalDateTime.of(2026, 10, 22, 20, 0);
        assertThat(NotificationPreferenceService.deliverAfter(quiet, JAKARTA, night))
                .isEqualTo(LocalDateTime.of(2026, 10, 23, 0, 0));
        // 12:00 in Jakarta -> now.
        LocalDateTime noon = LocalDateTime.of(2026, 10, 22, 5, 0);
        assertThat(NotificationPreferenceService.deliverAfter(quiet, JAKARTA, noon)).isEqualTo(noon);
    }

    private static NotificationPreference row(NotificationType type, NotificationChannel channel, boolean enabled,
                                              LocalTime quietStart, LocalTime quietEnd) {
        NotificationPreference row = new NotificationPreference();
        row.setMemberId(MEMBER);
        row.setType(type);
        row.setChannel(channel);
        row.setEnabled(enabled);
        row.setQuietStart(quietStart);
        row.setQuietEnd(quietEnd);
        return row;
    }
}
