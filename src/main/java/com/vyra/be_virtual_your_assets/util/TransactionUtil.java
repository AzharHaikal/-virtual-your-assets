package com.vyra.be_virtual_your_assets.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class TransactionUtil {
    private static final String VYRA = "VYRA";

    private TransactionUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String generateReferenceNumber() {
        return VYRA +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) +
                ThreadLocalRandom.current().nextInt(100, 999);
    }
}
