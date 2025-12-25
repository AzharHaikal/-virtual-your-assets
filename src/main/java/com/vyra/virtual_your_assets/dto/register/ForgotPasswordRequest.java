package com.vyra.virtual_your_assets.dto.register;

import com.vyra.virtual_your_assets.constant.OtpType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank
    private String phoneNumber;
}
