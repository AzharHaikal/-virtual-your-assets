package com.vyra.be_virtual_your_assets.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPinRequest {
    @Pattern(regexp = "^62\\d+$", message = "Phone number must start with 62")
    @Size(min = 10, max = 15, message = "Phone number must contain 10 to 15 digits")
    private String phoneNumber;

    @Email(message = "Please enter a valid email address")
    @Size(max = 30, message = "Email address must not exceed 30 characters")
    private String email;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{6}$", message = "PIN must consist of exactly 6 digits")
    private String newPin;

    @JsonIgnore
    @AssertTrue(message = "Email address or phone number is required")
    public boolean isIdentifierProvided() {
        return (email != null && !email.isBlank()) || (phoneNumber != null && !phoneNumber.isBlank());
    }
}
