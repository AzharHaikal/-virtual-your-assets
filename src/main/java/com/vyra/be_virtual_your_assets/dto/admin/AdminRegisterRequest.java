package com.vyra.be_virtual_your_assets.dto.admin;

import com.vyra.be_virtual_your_assets.constant.AdminRole;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminRegisterRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 50, message = "Email address must not exceed 50 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^62\\d+$", message = "Phone number must start with 62")
    @Size(min = 10, max = 15, message = "Phone number must contain 10 to 15 digits")
    private String phoneNumber;

    @NotNull(message = "Role is required")
    private AdminRole role;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    private String password;
}
