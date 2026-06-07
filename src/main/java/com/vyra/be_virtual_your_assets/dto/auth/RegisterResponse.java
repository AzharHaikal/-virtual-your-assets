package com.vyra.be_virtual_your_assets.dto.auth;

import lombok.Data;

@Data
public class RegisterResponse {
    private String memberId;
    private String fullName;
    private String email;
    private String phoneNumber;

}