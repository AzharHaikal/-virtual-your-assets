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
     * report. SUPER_ADMIN only; no setting can switch it off for the SUPER_ADMIN.
     */
    MEMBER_TRANSACTION_VIEW,
    /**
     * Member-level wellbeing data: mood check-ins (including the journal note), habits and goals
     * of each member (the Mood, Habit and Goal reports). SUPER_ADMIN only; every other role is
     * denied.
     */
    MEMBER_WELLBEING_VIEW,

    REPORT_VIEW,
    /**
     * Bulk download (Excel/CSV): list endpoints called with a page size above
     * {@code AdminAuthorizationService.BULK_PAGE_SIZE}. SUPER_ADMIN and ADMINISTRATOR.
     */
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
