package com.vyra.be_virtual_your_assets.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminRefreshTokenResponse {
    private String accessToken;
    private String refreshToken;
    private LocalDateTime accessTokenExpiredAt;
    private LocalDateTime refreshTokenExpiredAt;
}
