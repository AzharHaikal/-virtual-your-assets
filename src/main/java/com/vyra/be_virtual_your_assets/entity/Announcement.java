package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.AnnouncementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A message from VYRA to its members: a banner while active, and one ANNOUNCEMENT notification. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "admin_idp", name = "announcement")
public class Announcement extends BaseEntity {

    /** Shown to every language. */
    public static final String ALL_LANGUAGES = "ALL";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    /** ALL, ID or EN (member.language). */
    @Column(nullable = false, length = 3)
    private String language = ALL_LANGUAGES;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AnnouncementStatus status = AnnouncementStatus.DRAFT;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    /** null = until unpublished. */
    private LocalDateTime endsAt;

    private LocalDateTime publishedAt;

    /** When the notifications were sent (once). */
    private LocalDateTime deliveredAt;

    public boolean isActiveAt(LocalDateTime now) {
        return status == AnnouncementStatus.PUBLISHED && getDeletedAt() == null
                && !startsAt.isAfter(now) && (endsAt == null || endsAt.isAfter(now));
    }
}
