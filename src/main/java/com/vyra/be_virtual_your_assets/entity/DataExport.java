package com.vyra.be_virtual_your_assets.entity;

import com.vyra.be_virtual_your_assets.constant.DataExportStatus;
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

/** An export of all the member's personal data (ZIP), kept for 24 hours once ready. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "idp", name = "data_export")
public class DataExport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DataExportStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;

    /** SHA-256 (hex) of the token in the e-mailed link; the token itself is never stored. */
    @Column(length = 64)
    private String downloadTokenHash;

    /** The ZIP; emptied when the export expires. */
    @Column(columnDefinition = "bytea")
    private byte[] content;

    private Long sizeBytes;

    @Column(length = 255)
    private String error;
}
