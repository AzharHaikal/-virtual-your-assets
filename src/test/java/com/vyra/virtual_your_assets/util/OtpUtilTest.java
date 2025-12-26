package com.vyra.virtual_your_assets.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@ExtendWith(MockitoExtension.class)
class OtpUtilTest {

    @Test
    void testConstructorIsPrivate() throws NoSuchMethodException {
        Constructor<OtpUtil> constructor = OtpUtil.class.getDeclaredConstructor();

        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));

        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    void generateOtpSuccess() {
        String otp = OtpUtil.generateOtp();

        assertNotNull(otp);
        assertEquals(6, otp.length(), "OTP harus berjumlah 6 digit");
        assertTrue(otp.matches("\\d{6}"), "OTP harus hanya berisi angka");
    }

    @Test
    void getExpiredTimeSuccess() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime expiredTime = OtpUtil.getExpiredTime();

        assertNotNull(expiredTime);
        assertTrue(expiredTime.isAfter(now.plusMinutes(4).plusSeconds(55)));
        assertTrue(expiredTime.isBefore(now.plusMinutes(5).plusSeconds(5)));
    }
}