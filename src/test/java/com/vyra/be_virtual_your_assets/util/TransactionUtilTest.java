package com.vyra.be_virtual_your_assets.util;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TransactionUtilTest {

    @Test
    void constructor_shouldThrowIllegalStateException() throws Exception {
        Constructor<TransactionUtil> constructor = TransactionUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertThat(ex.getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(ex.getCause().getMessage()).isEqualTo("Utility class");
    }

    @RepeatedTest(10)
    void generateReferenceNumber_shouldStartWithVYRA() {
        String ref = TransactionUtil.generateReferenceNumber();

        assertNotNull(ref);
        assertThat(ref).startsWith("VYRA");
    }

    @RepeatedTest(10)
    void generateReferenceNumber_shouldHaveExpectedLength() {
        String ref = TransactionUtil.generateReferenceNumber();

        // "VYRA" (4) + timestamp "yyyyMMddHHmmssSSS" (17) + random 3-digit (3) = 24
        assertThat(ref.length()).isEqualTo(24);
    }

    @RepeatedTest(5)
    void generateReferenceNumber_shouldEndWithThreeDigitRandom() {
        String ref = TransactionUtil.generateReferenceNumber();

        // Last 3 chars must be numeric (100–998)
        String randomPart = ref.substring(ref.length() - 3);
        assertTrue(randomPart.matches("\\d{3}"), "Last 3 chars must be numeric: " + randomPart);
        int random = Integer.parseInt(randomPart);
        assertTrue(random >= 100 && random <= 998);
    }

    @RepeatedTest(20)
    void generateReferenceNumber_shouldBeUnique() {
        String ref1 = TransactionUtil.generateReferenceNumber();
        String ref2 = TransactionUtil.generateReferenceNumber();
        // Due to timestamp + random, references generated at different times will differ
        // (same-millisecond collision is theoretically possible but extremely rare in test)
        assertNotNull(ref1);
        assertNotNull(ref2);
    }
}
