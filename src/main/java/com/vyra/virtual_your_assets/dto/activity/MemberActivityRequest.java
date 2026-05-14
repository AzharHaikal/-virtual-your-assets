package com.vyra.virtual_your_assets.dto.activity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
public class MemberActivityRequest {
    private String phoneNumber;
    private String description;
    private LocalDateTime createdAt;

}
