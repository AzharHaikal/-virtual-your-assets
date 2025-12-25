package com.vyra.virtual_your_assets.dto.register;

import com.vyra.virtual_your_assets.constant.OtpType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    private String phoneNumber;
    @NotNull
    private String otpCode;
    @NotNull
    private OtpType otpType;
}
