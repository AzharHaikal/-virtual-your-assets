package com.vyra.virtual_your_assets.dto.register;

import com.vyra.virtual_your_assets.constant.OtpType;
import lombok.Data;

@Data
public class ResendOtpRequest {
    private String email;
    private OtpType otpType;
}
