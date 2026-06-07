package com.vyra.virtual_your_assets.dto.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginResponse {
    private String memberId;
    private String phoneNumber;
    private String email;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime accessTokenExpiredAt;
    private LocalDateTime refreshTokenExpiredAt;
}
