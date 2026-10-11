package com.vyra.be_virtual_your_assets.dto.audit;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemberAuditItemResponse {
    private String id;
    private String phoneNumber;
    private String event;
    private String description;
    private LocalDateTime createdAt;
}
