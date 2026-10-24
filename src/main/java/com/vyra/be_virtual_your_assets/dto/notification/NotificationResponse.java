package com.vyra.be_virtual_your_assets.dto.notification;

import com.vyra.be_virtual_your_assets.constant.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class NotificationResponse {
    private String id;
    private NotificationType type;
    /** In the member's language at the time it was created. */
    private String title;
    private String body;
    /** Deep link: "route" (habit, budget, goal, ...) plus the ids it needs. */
    private Map<String, String> payload;
    /** null while unread. */
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
