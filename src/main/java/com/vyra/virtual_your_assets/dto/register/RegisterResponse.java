package com.vyra.virtual_your_assets.dto.register;

import lombok.Data;

import java.util.UUID;

@Data
public class RegisterResponse {
    private UUID memberId;
    private String email;
    private String phoneNumber;

}