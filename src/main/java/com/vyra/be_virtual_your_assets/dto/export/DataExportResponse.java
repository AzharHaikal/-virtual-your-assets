package com.vyra.be_virtual_your_assets.dto.export;

import com.vyra.be_virtual_your_assets.constant.DataExportStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DataExportResponse {
    private String id;
    private DataExportStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    /** Download possible until then (READY only). */
    private LocalDateTime expiresAt;
    private Long sizeBytes;
}
