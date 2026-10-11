package com.vyra.be_virtual_your_assets.dto.audit;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAuditItemResponse {
    private String id;
    private String adminId;
    private String adminName;
    private String event;
    private String description;
    private String ipAddress;
    private String device;
    private LocalDateTime createdAt;
}
