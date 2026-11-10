package com.vyra.be_virtual_your_assets.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    /**
     * What is stored and looked up in {@code member_token} / {@code admin_token}: the SHA-256 of the token
     * (Base64URL, 43 chars). The raw token only exists in the response and on the client, so a leaked
     * database or backup cannot be replayed as a session.
     */
    public static String hash(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String generateSecureToken(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}