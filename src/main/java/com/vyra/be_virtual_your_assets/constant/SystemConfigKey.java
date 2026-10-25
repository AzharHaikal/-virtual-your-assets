package com.vyra.be_virtual_your_assets.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * Settings stored in {@code admin_idp.system_config} and managed from the portal.
 * Only keys listed here can be read or changed; {@link #defaultValue} applies when the row is missing.
 */
@Getter
@AllArgsConstructor
public enum SystemConfigKey {

    MEMBER_TRANSACTION_DETAIL_ENABLED(
            "privacy.member_transaction_detail.enabled",
            ValueType.BOOLEAN,
            "true",
            "Allow Super Admin to view member-level transactions and balances"
    ),

    MAINTENANCE_ENABLED(
            "maintenance.enabled",
            ValueType.BOOLEAN,
            "false",
            "Maintenance mode: the member app gets 503 and shows the maintenance message (admin portal keeps working)"
    ),

    MAINTENANCE_MESSAGE(
            "maintenance.message",
            ValueType.TEXT,
            "",
            "Message shown in the app during maintenance (empty = the app's default text)"
    ),

    OTP_EMAIL_ENABLED(
            "otp.email.enabled",
            ValueType.BOOLEAN,
            "true",
            "Send verification codes by e-mail"
    ),

    OTP_WHATSAPP_ENABLED(
            "otp.whatsapp.enabled",
            ValueType.BOOLEAN,
            "false",
            "Send verification codes by WhatsApp (needs a WhatsApp Business API provider)"
    );

    private final String key;
    private final ValueType type;
    private final String defaultValue;
    private final String description;

    public static Optional<SystemConfigKey> fromKey(String key) {
        return Arrays.stream(values()).filter(k -> k.key.equals(key)).findFirst();
    }

    public enum ValueType {
        BOOLEAN,
        /** Free text, at most 500 characters. */
        TEXT
    }
}
