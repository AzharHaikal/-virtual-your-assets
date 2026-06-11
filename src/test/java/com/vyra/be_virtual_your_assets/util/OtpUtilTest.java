package com.vyra.be_virtual_your_assets.util;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OtpUtilTest {

    @Test
    void constructor_shouldThrowIllegalStateException() throws Exception {
        Constructor<OtpUtil> constructor = OtpUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertThat(ex.getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(ex.getCause().getMessage()).isEqualTo("Utility class");
    }

    @RepeatedTest(20)
    void generateOtp_shouldReturn6DigitNumericString() {
        String otp = OtpUtil.generateOtp();

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"), "OTP must be 6 digits: " + otp);
        int value = Integer.parseInt(otp);
        assertTrue(value >= 100000 && value <= 999999);
    }

    @Test
    void getExpiredTime_shouldReturnFiveMinutesFromNow() {
        LocalDateTime before = LocalDateTime.now().plusMinutes(5).minusSeconds(1);
        LocalDateTime expiredAt = OtpUtil.getExpiredTime();
        LocalDateTime after = LocalDateTime.now().plusMinutes(5).plusSeconds(1);

        assertNotNull(expiredAt);
        assertTrue(expiredAt.isAfter(before), "expiredAt should be after " + before);
        assertTrue(expiredAt.isBefore(after), "expiredAt should be before " + after);
    }
}
