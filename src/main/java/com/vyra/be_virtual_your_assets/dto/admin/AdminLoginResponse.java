package com.vyra.be_virtual_your_assets.dto.admin;

import com.vyra.be_virtual_your_assets.constant.AdminRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class AdminLoginResponse {
    private String adminId;
    private String name;
    private String email;
    private AdminRole role;
    private Set<String> permissions;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime accessTokenExpiredAt;
    private LocalDateTime refreshTokenExpiredAt;
}
