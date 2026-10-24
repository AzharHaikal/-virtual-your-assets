package com.vyra.be_virtual_your_assets.dto.auth;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/** Optional body of the logout: the device's push token, removed so it gets no more notifications. */
@Data
public class LogoutRequest {
    @Size(max = 512, message = "Invalid token")
    @ToString.Exclude
    private String pushToken;
}
