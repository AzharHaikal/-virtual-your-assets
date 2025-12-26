package com.vyra.virtual_your_assets.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;

public class OtpUtil {

    private OtpUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String generateOtp() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }

    public static LocalDateTime getExpiredTime() {
        return LocalDateTime.now().plusMinutes(5);
    }
}
