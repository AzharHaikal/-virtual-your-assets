package com.vyra.virtual_your_assets.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

@ExtendWith(MockitoExtension.class)
class TokenUtilTest {

    @Test
    void testConstructorIsPrivate() throws NoSuchMethodException {
        Constructor<TokenUtil> constructor = TokenUtil.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor harus private");

        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance,
                "Constructor harus melempar exception saat dipanggil");
    }

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