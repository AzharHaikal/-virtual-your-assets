package com.vyra.be_virtual_your_assets.dto.auth;

import com.vyra.be_virtual_your_assets.constant.OtpType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank(message = "Email address is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 30, message = "Email address must not exceed 30 characters")
    private String email;

    @NotNull
    private String otpCode;

    @NotNull
    private OtpType otpType;
}
