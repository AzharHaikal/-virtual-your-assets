package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * A notification in the member's inbox; the source of truth for every channel. Created only through
 * {@code NotificationRepository.insertIfAbsent} so the dedup key makes it idempotent.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "notification", name = "notification")
public class Notification {

    @Id
    private String id;

    @Column(nullable = false)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    /** May contain habit / goal / category names: never logged. */
    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    /** Deep link for the app, e.g. {"route": "habit", "habitId": "..."}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> payload;

    @Column(nullable = false, length = 200)
    private String dedupKey;

    private LocalDateTime readAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
