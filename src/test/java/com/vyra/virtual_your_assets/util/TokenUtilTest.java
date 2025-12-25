package com.vyra.virtual_your_assets.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class TokenUtilTest {

    @Test
    void generateTokenSuccess() {
        String token = TokenUtil.generateToken();

        assertNotNull(token);
        assertEquals(32, token.length(), "Token harus berjumlah 32 karakter hexadecimal");
        assertFalse(token.contains("-"), "Token tidak boleh mengandung tanda hubung");
    }

    @Test
    void expiredAtSuccess() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime expired = TokenUtil.expiredAt();

        assertNotNull(expired);
        assertTrue(expired.isAfter(now.plusHours(11).plusMinutes(59).plusSeconds(55)));
        assertTrue(expired.isBefore(now.plusHours(12).plusSeconds(5)));
    }
}