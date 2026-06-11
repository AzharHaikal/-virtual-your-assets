package com.vyra.be_virtual_your_assets.util;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TokenUtilTest {

    @Test
    void constructor_shouldThrowIllegalStateException() throws Exception {
        Constructor<TokenUtil> constructor = TokenUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertThat(ex.getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(ex.getCause().getMessage()).isEqualTo("Utility class");
    }

    @RepeatedTest(10)
    void generateAccessToken_shouldReturnNonNullBase64UrlEncodedToken() {
        String token = TokenUtil.generateAccessToken();

        assertNotNull(token);
        assertFalse(token.isBlank());
        // Must be URL-safe Base64 without padding: no +, /, =
        assertFalse(token.contains("+"), "access token must not contain '+'");
        assertFalse(token.contains("/"), "access token must not contain '/'");
        assertFalse(token.contains("="), "access token must not contain '='");
        // 32 bytes → 43 URL-safe Base64 chars (ceil(32/3)*4 = 44, minus 1 padding = 43)
        byte[] decoded = Base64.getUrlDecoder().decode(token + "="); // add pad for decoding
        assertThat(decoded.length).isGreaterThanOrEqualTo(31); // flexible due to padding removal
    }

    @RepeatedTest(10)
    void generateRefreshToken_shouldReturnLongerTokenThanAccessToken() {
        String accessToken = TokenUtil.generateAccessToken();
        String refreshToken = TokenUtil.generateRefreshToken();

        assertNotNull(refreshToken);
        assertFalse(refreshToken.isBlank());
        assertFalse(refreshToken.contains("+"));
        assertFalse(refreshToken.contains("/"));
        assertFalse(refreshToken.contains("="));
        // 64 bytes > 32 bytes → refresh token must be longer
        assertThat(refreshToken.length()).isGreaterThan(accessToken.length());
    }

    @RepeatedTest(5)
    void generateAccessToken_shouldProduceUniqueTokens() {
        String t1 = TokenUtil.generateAccessToken();
        String t2 = TokenUtil.generateAccessToken();
        assertNotEquals(t1, t2);
    }

    @RepeatedTest(5)
    void generateRefreshToken_shouldProduceUniqueTokens() {
        String t1 = TokenUtil.generateRefreshToken();
        String t2 = TokenUtil.generateRefreshToken();
        assertNotEquals(t1, t2);
    }

    @Test
    void accessTokenExpiredAt_shouldReturnFiveMinutesFromNow() {
        LocalDateTime before = LocalDateTime.now().plusMinutes(5).minusSeconds(1);
        LocalDateTime expiredAt = TokenUtil.accessTokenExpiredAt();
        LocalDateTime after = LocalDateTime.now().plusMinutes(5).plusSeconds(1);

        assertNotNull(expiredAt);
        assertTrue(expiredAt.isAfter(before));
        assertTrue(expiredAt.isBefore(after));
    }

    @Test
    void refreshTokenExpiredAt_shouldReturnThirtyDaysFromNow() {
        LocalDateTime before = LocalDateTime.now().plusDays(30).minusSeconds(1);
        LocalDateTime expiredAt = TokenUtil.refreshTokenExpiredAt();
        LocalDateTime after = LocalDateTime.now().plusDays(30).plusSeconds(1);

        assertNotNull(expiredAt);
        assertTrue(expiredAt.isAfter(before));
        assertTrue(expiredAt.isBefore(after));
    }
}
