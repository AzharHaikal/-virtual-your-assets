package com.vyra.be_virtual_your_assets.constant;

import java.util.Set;

/** Currencies the app can format amounts in (display only, no exchange rates). */
public final class AppCurrency {
    public static final String DEFAULT = "IDR";
    public static final Set<String> SUPPORTED = Set.of("IDR", "USD", "EUR", "SGD", "MYR", "JPY", "AUD");

    private AppCurrency() {
    }
}
