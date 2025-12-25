package com.vyra.virtual_your_assets.dto.login;

import lombok.Data;

@Data
public class LoginRequest {
    private String identifier;
    private String pin;
}
