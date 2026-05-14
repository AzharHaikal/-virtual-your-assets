package com.vyra.virtual_your_assets.dto.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RefreshTokenResponse {
    private String accessToken;
    private LocalDateTime accessTokenExpiredAt;

}