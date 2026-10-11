package com.vyra.be_virtual_your_assets.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberActivityResponse {
    private String id;
    private String phoneNumber;
    private String event;
    private String description;
    private LocalDateTime createdAt;
}
