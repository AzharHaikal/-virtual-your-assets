package com.vyra.be_virtual_your_assets.dto.push;

import com.vyra.be_virtual_your_assets.constant.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class RegisterPushTokenRequest {
    /** FCM registration token of this device. */
    @NotBlank(message = "Token is required")
    @Size(max = 512, message = "Invalid token")
    @ToString.Exclude
    private String token;

    @NotNull(message = "Platform is required")
    private DevicePlatform platform;
}
