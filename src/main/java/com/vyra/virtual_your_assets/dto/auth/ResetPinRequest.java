package com.vyra.virtual_your_assets.dto.auth;

import lombok.Data;

@Data
public class ResetPinRequest {
    private String phoneNumber;
    private String email;
    private String newPin;
}
