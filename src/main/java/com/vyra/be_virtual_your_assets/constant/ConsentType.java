package com.vyra.be_virtual_your_assets.constant;

/** Explicit opt-ins a member can give and take back at any time. */
public enum ConsentType {
    /** Weekly aggregates (no journal, names or amounts) may be sent to the AI model. */
    AI_REFLECTION;

    /** null for unknown text. */
    public static ConsentType parse(String value) {
        for (ConsentType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
