package com.vyra.be_virtual_your_assets.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;

@Data
public class VerifyContactChangeRequest {
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must consist of exactly 6 digits")
    @ToString.Exclude
    private String otpCode;
}
