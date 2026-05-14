package com.vyra.virtual_your_assets.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotNull
    private String refreshToken;
}