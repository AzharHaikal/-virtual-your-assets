package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.UserReportCategory;
import com.vyra.be_virtual_your_assets.constant.UserReportStatus;
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

/** A problem or abuse reported by a member, handled by admins with REPORT_HANDLE. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "idp", name = "user_report")
public class UserReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserReportCategory category;

    @Column(nullable = false, length = 2000)
    private String description;

    /** A screenshot is stored in {@link UserReportAttachment}. */
    @Column(nullable = false)
    private boolean hasAttachment;

    @Column(length = 30)
    private String appVersion;

    @Column(length = 20)
    private String platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private UserReportStatus status = UserReportStatus.OPEN;

    private String assignedAdminId;

    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
