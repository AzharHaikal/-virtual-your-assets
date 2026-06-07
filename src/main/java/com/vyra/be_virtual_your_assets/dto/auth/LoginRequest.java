package com.vyra.be_virtual_your_assets.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {
    @Email(message = "Please enter a valid email address")
    @Size(max = 30, message = "Email address must not exceed 30 characters")
    private String email;

    @Pattern(regexp = "^62\\d+$", message = "Phone number must start with 62")
    @Size(min = 10, max = 15, message = "Phone number must contain 10 to 15 digits")
    private String phoneNumber;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{6}$", message = "PIN must consist of exactly 6 digits")
    private String pin;

    private String deviceId;
    private String deviceName;
    private String ipAddress;

    @AssertTrue(message = "Email or phone number is required")
    public boolean isOnlyOneIdentifierFilled() {
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phoneNumber != null && !phoneNumber.isBlank();
        // return hasEmail || hasPhone;
        return hasEmail ^ hasPhone;
    }
}