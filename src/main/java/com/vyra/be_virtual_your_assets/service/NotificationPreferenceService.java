package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.NotificationChannel;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationPreferenceItem;
import com.vyra.be_virtual_your_assets.dto.notification.NotificationPreferencesResponse;
import com.vyra.be_virtual_your_assets.dto.notification.UpdateNotificationPreferencesRequest;
import com.vyra.be_virtual_your_assets.entity.NotificationPreference;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Which notification types a member wants on which channel, and their quiet hours (local time, may pass
 * midnight). Without a stored choice IN_APP and PUSH are on and EMAIL is off. A notification that falls in
 * quiet hours is delivered when they end, not dropped.
 */
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final TimeService timeService;

    /** The member's choices with the defaults filled in. */
    public record Preferences(Map<NotificationType, Map<NotificationChannel, Boolean>> enabled,
                              LocalTime quietStart, LocalTime quietEnd) {

        public boolean isEnabled(NotificationType type, NotificationChannel channel) {
            return enabled.get(type).get(channel);
        }

        public boolean hasQuietHours() {
            return quietStart != null && quietEnd != null;
        }
    }

    @Transactional(readOnly = true)
    public Preferences resolve(String memberId) {
        Map<NotificationType, Map<NotificationChannel, Boolean>> enabled = new EnumMap<>(NotificationType.class);
        for (NotificationType type : NotificationType.values()) {
            Map<NotificationChannel, Boolean> channels = new EnumMap<>(NotificationChannel.class);
            for (NotificationChannel channel : NotificationChannel.values()) {
                channels.put(channel, channel.isEnabledByDefault());
            }
            enabled.put(type, channels);
        }
        LocalTime quietStart = null;
        LocalTime quietEnd = null;
        for (NotificationPreference row : preferenceRepository.findByMemberId(memberId)) {
            enabled.get(row.getType()).put(row.getChannel(), row.isEnabled());
            if (row.getQuietStart() != null) {
                quietStart = row.getQuietStart();
                quietEnd = row.getQuietEnd();
            }
        }
        return new Preferences(enabled, quietStart, quietEnd);
    }

    @Transactional(readOnly = true)
    public BaseResponse<NotificationPreferencesResponse> getPreferences(String memberId) {
        return new BaseResponse<>(ErrorConstant.NOTIFICATION_PREFERENCES_SUCCESS.getCode(),
                ErrorConstant.NOTIFICATION_PREFERENCES_SUCCESS.getMessage(), toResponse(resolve(memberId)));
    }

    /** Stores every type x channel row, so the quiet hours are on all of them. */
    @Transactional
    public BaseResponse<NotificationPreferencesResponse> updatePreferences(String memberId,
                                                                          UpdateNotificationPreferencesRequest request) {
        LocalTime quietStart = minutes(request.getQuietStart());
        LocalTime quietEnd = minutes(request.getQuietEnd());
        if ((quietStart == null) != (quietEnd == null) || (quietStart != null && quietStart.equals(quietEnd))) {
            throw new BadRequestException(ErrorConstant.NOTIFICATION_QUIET_HOURS_INVALID);
        }
        Preferences current = resolve(memberId);
        if (request.getItems() != null) {
            for (NotificationPreferenceItem item : request.getItems()) {
                current.enabled().get(item.getType()).put(item.getChannel(), item.getEnabled());
            }
        }

        LocalDateTime now = timeService.nowUtc();
        List<NotificationPreference> rows = new ArrayList<>();
        for (NotificationType type : NotificationType.values()) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                NotificationPreference row = new NotificationPreference();
                row.setMemberId(memberId);
                row.setType(type);
                row.setChannel(channel);
                row.setEnabled(current.isEnabled(type, channel));
                row.setQuietStart(quietStart);
                row.setQuietEnd(quietEnd);
                row.setUpdatedAt(now);
                rows.add(row);
            }
        }
        preferenceRepository.saveAll(rows);
        return new BaseResponse<>(ErrorConstant.NOTIFICATION_PREFERENCES_SAVED.getCode(),
                ErrorConstant.NOTIFICATION_PREFERENCES_SAVED.getMessage(),
                toResponse(new Preferences(current.enabled(), quietStart, quietEnd)));
    }

    /**
     * When a notification created at {@code nowUtc} may be delivered outside the app: now, or the end of
     * the quiet hours in the member's zone.
     */
    public static LocalDateTime deliverAfter(Preferences preferences, ZoneId zone, LocalDateTime nowUtc) {
        if (!preferences.hasQuietHours()) {
            return nowUtc;
        }
        ZonedDateTime local = nowUtc.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone);
        LocalTime time = local.toLocalTime();
        if (!isQuiet(time, preferences.quietStart(), preferences.quietEnd())) {
            return nowUtc;
        }
        // Before the end today (same-day window, or the morning part of one that passes midnight): today.
        LocalDate endDay = time.isBefore(preferences.quietEnd()) ? local.toLocalDate() : local.toLocalDate().plusDays(1);
        return endDay.atTime(preferences.quietEnd()).atZone(zone)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** [start, end) in local time; a window with end before start passes midnight. */
    static boolean isQuiet(LocalTime time, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        return !time.isBefore(start) || time.isBefore(end);
    }

    private static LocalTime minutes(LocalTime time) {
        return time == null ? null : time.truncatedTo(ChronoUnit.MINUTES);
    }

    private static NotificationPreferencesResponse toResponse(Preferences preferences) {
        List<NotificationPreferenceItem> items = new ArrayList<>();
        for (NotificationType type : NotificationType.values()) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                items.add(new NotificationPreferenceItem(type, channel, preferences.isEnabled(type, channel)));
            }
        }
        NotificationPreferencesResponse response = new NotificationPreferencesResponse();
        response.setQuietStart(preferences.quietStart());
        response.setQuietEnd(preferences.quietEnd());
        response.setItems(items);
        return response;
    }
}
