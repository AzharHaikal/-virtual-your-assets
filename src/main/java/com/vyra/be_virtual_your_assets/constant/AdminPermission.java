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
    /**
     * Member-level financial data: a member's transactions, balance and the per-transaction
     * report. SUPER_ADMIN only, and only while the system setting
     * {@link SystemConfigKey#MEMBER_TRANSACTION_DETAIL_ENABLED} is on.
     */
    MEMBER_TRANSACTION_VIEW,

    REPORT_VIEW,
    REPORT_EXPORT,

    AUDIT_VIEW,

    SETTINGS_VIEW,
    SETTINGS_MANAGE,

    /** Built-in transaction categories shared by every member. */
    CATEGORY_MANAGE,
    /** Announcements shown to members (banner + notification). */
    ANNOUNCEMENT_MANAGE,
    /** The queue of problems / abuse reported by members; never opens their private data. */
    REPORT_HANDLE
}
