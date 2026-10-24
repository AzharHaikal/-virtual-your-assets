package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.NotificationChannel;
import com.vyra.be_virtual_your_assets.constant.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Whether a member wants one type of notification on one channel; quiet hours are the member's local time. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@IdClass(NotificationPreference.Key.class)
@Table(schema = "notification", name = "notification_preference")
public class NotificationPreference {

    @Id
    private String memberId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationType type;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private NotificationChannel channel;

    @Column(nullable = false)
    private boolean enabled;

    private LocalTime quietStart;
    private LocalTime quietEnd;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private String memberId;
        private NotificationType type;
        private NotificationChannel channel;
    }
}
