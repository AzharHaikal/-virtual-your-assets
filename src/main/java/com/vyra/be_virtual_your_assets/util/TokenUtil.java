package com.vyra.be_virtual_your_assets.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

public class TokenUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String generateAccessToken() {
        return generateSecureToken(32);
    }

    public static String generateRefreshToken() {
        return generateSecureToken(64);
    }

    public static LocalDateTime accessTokenExpiredAt() {
        return LocalDateTime.now().plusMinutes(5);
    }

    public static LocalDateTime refreshTokenExpiredAt() {
        return LocalDateTime.now().plusDays(30);
    }

    private static String generateSecureToken(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}