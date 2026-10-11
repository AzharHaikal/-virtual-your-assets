package com.vyra.be_virtual_your_assets.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminRefreshTokenRequest {
    @NotNull(message = "Refresh token is required")
    private String refreshToken;
}
