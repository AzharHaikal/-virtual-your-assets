package com.vyra.be_virtual_your_assets.dto.notification;

import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Replaces the quiet hours (both or none) and changes the listed type x channel combinations; the ones
 * not listed keep their current value.
 */
@Data
public class UpdateNotificationPreferencesRequest {
    private LocalTime quietStart;
    private LocalTime quietEnd;

    private List<@Valid NotificationPreferenceItem> items = new ArrayList<>();
}
