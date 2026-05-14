package com.vyra.virtual_your_assets.dto.auth;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String phoneNumber;
    private String newPin;
}
