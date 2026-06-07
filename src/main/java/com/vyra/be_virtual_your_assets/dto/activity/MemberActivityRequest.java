package com.vyra.be_virtual_your_assets.dto.activity;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public class MemberActivityRequest {
    private String phoneNumber;
    private String description;
    private LocalDateTime createdAt;

}
