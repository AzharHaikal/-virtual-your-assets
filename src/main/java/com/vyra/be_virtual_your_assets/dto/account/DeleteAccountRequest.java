package com.vyra.be_virtual_your_assets.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;

/** Step 1 of deleting the own account: the PIN, after which an OTP is e-mailed. */
@Data
public class DeleteAccountRequest {
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{6}$", message = "PIN must consist of exactly 6 digits")
    @ToString.Exclude
    private String pin;
}
