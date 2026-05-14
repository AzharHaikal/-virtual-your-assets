package com.vyra.virtual_your_assets.dto.member;

import lombok.Data;

@Data
public class UpdateProfileResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
