package com.vyra.virtual_your_assets.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePinRequest {
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{6}$", message = "PIN must consist of exactly 6 digits")
    private String oldPin;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{6}$", message = "PIN must consist of exactly 6 digits")
    private String newPin;
}
