package com.vyra.be_virtual_your_assets.dto.admin;

import com.vyra.be_virtual_your_assets.constant.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateRequest {
    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    @Email(message = "Please enter a valid email address")
    @Size(max = 50, message = "Email address must not exceed 50 characters")
    private String email;

    @Pattern(regexp = "^62\\d+$", message = "Phone number must start with 62")
    @Size(min = 10, max = 15, message = "Phone number must contain 10 to 15 digits")
    private String phoneNumber;

    private AdminRole role;
}
