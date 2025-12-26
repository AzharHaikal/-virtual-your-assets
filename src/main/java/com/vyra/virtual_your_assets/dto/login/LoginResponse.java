package com.vyra.virtual_your_assets.dto.login;

import lombok.Data;

@Data
public class LoginResponse {
    private String phoneNumber;
    private String email;
    private String token;

}
