package com.vyra.be_virtual_your_assets.constant;

import java.util.Locale;
import java.util.Optional;

/** Legal documents shown in the app and on the landing page. */
public enum LegalDocumentType {
    PRIVACY,
    TERMS;

    /** "privacy" / "TERMS" (case-insensitive). */
    public static Optional<LegalDocumentType> parse(String value) {
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
