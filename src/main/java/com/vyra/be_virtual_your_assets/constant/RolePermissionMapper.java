package com.vyra.be_virtual_your_assets.constant;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Static mapping between {@link AdminRole} and the set of {@link AdminPermission}
 * each role is allowed to perform. Used by AdminService to expose permissions
 * to the portal (for menu rendering) and by AdminAuthorizationFilter to guard
 * write operations.
 */
public final class RolePermissionMapper {

    private static final Map<AdminRole, Set<AdminPermission>> ROLE_PERMISSIONS = Map.of(
            AdminRole.SUPER_ADMIN, EnumSet.allOf(AdminPermission.class),

            AdminRole.ADMINISTRATOR, EnumSet.of(
                    AdminPermission.DASHBOARD_VIEW,
                    AdminPermission.USER_VIEW,
                    AdminPermission.USER_MANAGE,
                    AdminPermission.MEMBER_VIEW,
                    AdminPermission.MEMBER_MANAGE,
                    AdminPermission.REPORT_VIEW,
                    AdminPermission.REPORT_EXPORT,
                    AdminPermission.AUDIT_VIEW,
                    AdminPermission.SETTINGS_VIEW
            ),

            AdminRole.OPERATOR, EnumSet.of(
                    AdminPermission.DASHBOARD_VIEW,
                    AdminPermission.MEMBER_VIEW,
                    AdminPermission.MEMBER_MANAGE,
                    AdminPermission.REPORT_VIEW,
                    AdminPermission.AUDIT_VIEW,
                    AdminPermission.SETTINGS_VIEW
            ),

            AdminRole.VIEWER, EnumSet.of(
                    AdminPermission.DASHBOARD_VIEW,
                    AdminPermission.MEMBER_VIEW,
                    AdminPermission.REPORT_VIEW,
                    AdminPermission.AUDIT_VIEW,
                    AdminPermission.SETTINGS_VIEW
            )
    );

    private RolePermissionMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static Set<AdminPermission> getPermissions(AdminRole role) {
        return ROLE_PERMISSIONS.getOrDefault(role, EnumSet.noneOf(AdminPermission.class));
    }

    public static boolean hasPermission(AdminRole role, AdminPermission permission) {
        return getPermissions(role).contains(permission);
    }
}
