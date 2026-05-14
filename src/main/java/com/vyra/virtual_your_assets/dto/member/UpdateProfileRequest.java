package com.vyra.virtual_your_assets.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 20, message = "First name must not exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "First name may contain letters only")
    private String firstName;

    @Size(max = 20, message = "Last name must not exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Last name may contain letters only")
    private String lastName;

    @Email(message = "Please enter a valid email address")
    @Size(max = 30, message = "Email address must not exceed 30 characters")
    private String email;

    @Pattern(regexp = "^62\\d+$", message = "Phone number must start with 62")
    @Size(min = 10, max = 15, message = "Phone number must contain 10 to 15 digits")
    private String phoneNumber;

}