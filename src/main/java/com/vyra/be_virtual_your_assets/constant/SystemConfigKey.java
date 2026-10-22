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
    );

    private final String key;
    private final ValueType type;
    private final String defaultValue;
    private final String description;

    public static Optional<SystemConfigKey> fromKey(String key) {
        return Arrays.stream(values()).filter(k -> k.key.equals(key)).findFirst();
    }

    public enum ValueType {
        BOOLEAN
    }
}
