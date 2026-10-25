package com.vyra.be_virtual_your_assets.entity;

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

/** One step of handling a report: a status change and / or a note by an admin. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "idp", name = "user_report_note")
public class UserReportNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String reportId;

    @Column(nullable = false)
    private String adminId;

    /** The new status, when it changed. */
    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private UserReportStatus status;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
