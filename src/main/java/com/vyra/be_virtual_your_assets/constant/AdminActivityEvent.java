package com.vyra.be_virtual_your_assets.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Audit events emitted by administrators acting on the portal.
 * Kept fully separate from {@link MemberActivityEvent}, which tracks
 * mobile app member activity.
 */
@Getter
@AllArgsConstructor
public enum AdminActivityEvent {

    // LOGIN & LOGOUT
    ATTEMPT_LOGIN("Admin attempted to log in"),
    SUCCESS_LOGIN("Admin successfully logged in"),
    FAILED_LOGIN("Admin failed to log in"),
    LOGIN_LOCKED("Admin account temporarily locked after repeated failed logins"),
    SUCCESS_LOGOUT("Admin successfully logged out"),

    // ADMIN MANAGEMENT
    CREATE_ADMIN("Created a new admin account"),
    UPDATE_ADMIN("Updated an admin account"),
    DELETE_ADMIN("Deleted an admin account"),
    UPDATE_ADMIN_STATUS("Updated an admin account status"),
    RESET_ADMIN_PASSWORD("Reset an admin account password"),
    UPDATE_PROFILE("Updated own admin profile"),

    // MEMBER MANAGEMENT
    UPDATE_MEMBER_STATUS("Updated a member account status"),
    DELETE_MEMBER("Deleted a member account"),
    VIEW_MEMBER_TRANSACTIONS("Viewed member transactions"),

    // SETTINGS
    UPDATE_SYSTEM_CONFIG("Updated a system setting"),

    // REPORTS
    EXPORT_REPORT("Exported a report"),
    EXPORT_AUDIT_TRAIL("Exported audit trail data"),

    // PRODUCT CONFIGURATION (Phase 3)
    CREATE_CATEGORY("Created a built-in category"),
    UPDATE_CATEGORY("Updated a built-in category"),
    DEACTIVATE_CATEGORY("Deactivated a built-in category"),
    ACTIVATE_CATEGORY("Activated a built-in category"),
    CREATE_ANNOUNCEMENT("Created an announcement"),
    UPDATE_ANNOUNCEMENT("Updated an announcement"),
    PUBLISH_ANNOUNCEMENT("Published an announcement"),
    UNPUBLISH_ANNOUNCEMENT("Unpublished an announcement"),
    DELETE_ANNOUNCEMENT("Deleted an announcement"),
    PUBLISH_LEGAL_DOCUMENT("Published a legal document version"),
    UPDATE_USER_REPORT("Updated a user report");

    private final String description;
}
