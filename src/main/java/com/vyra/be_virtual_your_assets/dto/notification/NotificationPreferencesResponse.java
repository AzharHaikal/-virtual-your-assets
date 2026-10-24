package com.vyra.be_virtual_your_assets.dto.notification;

import lombok.Data;

import java.time.LocalTime;
import java.util.List;

/** Every type x channel combination (defaults filled in) and the quiet hours. */
@Data
public class NotificationPreferencesResponse {
    /** Local time (HH:mm, member's zone); both null = no quiet hours. May pass midnight (22:00 - 07:00). */
    private LocalTime quietStart;
    private LocalTime quietEnd;
    private List<NotificationPreferenceItem> items;
}
