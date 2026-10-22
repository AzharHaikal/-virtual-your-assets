package com.vyra.be_virtual_your_assets.util;

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

    /** How long a verified FORGOT_PIN OTP may be exchanged for a new PIN. */
    public static LocalDateTime getResetWindowExpiredTime() {
        return LocalDateTime.now().plusMinutes(5);
    }
}
