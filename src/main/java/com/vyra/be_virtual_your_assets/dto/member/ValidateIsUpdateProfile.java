package com.vyra.be_virtual_your_assets.dto.member;

import lombok.Data;

@Data
public class ValidateIsUpdateProfile {
    private boolean emailChanged;
    private boolean phoneChanged;
}
