package com.vyra.virtual_your_assets.util;

import java.time.LocalDateTime;
import java.util.UUID;

public class TokenUtil {
    public static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static LocalDateTime expiredAt() {
        return LocalDateTime.now().plusHours(12);
    }
}
