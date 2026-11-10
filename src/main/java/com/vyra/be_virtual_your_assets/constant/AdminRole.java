package com.vyra.be_virtual_your_assets.constant;

/**
 * Defines the system roles available for portal administrators.
 * Ordered from the highest privilege level to the lowest.
 */
public enum AdminRole {
    SUPER_ADMIN,
    ADMINISTRATOR,
    OPERATOR,
    VIEWER;

    /** True when this role is higher than {@code other} in the order above. */
    public boolean outranks(AdminRole other) {
        return other != null && ordinal() < other.ordinal();
    }
}
