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

    // REPORTS
    EXPORT_REPORT("Exported a report"),
    EXPORT_AUDIT_TRAIL("Exported audit trail data");

    private final String description;
}
