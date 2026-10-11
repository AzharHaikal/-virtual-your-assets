package com.vyra.be_virtual_your_assets.constant;

/**
 * Granular access rights that can be assigned to an {@link AdminRole}.
 * Used by the portal to decide which modules/actions an admin can access.
 */
public enum AdminPermission {
    DASHBOARD_VIEW,

    USER_VIEW,
    USER_MANAGE,

    MEMBER_VIEW,
    MEMBER_MANAGE,

    REPORT_VIEW,
    REPORT_EXPORT,

    AUDIT_VIEW,

    SETTINGS_VIEW,
    SETTINGS_MANAGE
}
