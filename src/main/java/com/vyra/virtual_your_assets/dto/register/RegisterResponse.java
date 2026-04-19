package com.vyra.virtual_your_assets.dto.register;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RegisterResponse {
    private String memberId;
    private String fullName;
    private String email;
    private String phoneNumber;

}