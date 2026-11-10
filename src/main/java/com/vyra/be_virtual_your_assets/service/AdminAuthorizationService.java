package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.RolePermissionMapper;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

/**
 * Centralized role-based access control (RBAC) helper for the admin module.
 * Every write/sensitive operation in AdminService and related services
 * should call {@link #requirePermission(AdminPermission)} before proceeding.
 * <p>
 * SUPER_ADMIN holds every permission and no setting restricts it; restrictions only apply to the other roles.
 */
@Service
public class AdminAuthorizationService {

    /** Largest page the portal screens request; anything bigger is an export and needs REPORT_EXPORT. */
    public static final int BULK_PAGE_SIZE = 100;

    public CustomAdminDetails getCurrentAdmin() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        if (!(principal instanceof CustomAdminDetails adminDetails)) {
            throw new ForbiddenException(ErrorConstant.ADMIN_UNAUTHORIZED);
        }
        return adminDetails;
    }

    public void requirePermission(AdminPermission permission) {
        CustomAdminDetails admin = getCurrentAdmin();
        if (!RolePermissionMapper.hasPermission(admin.getRole(), permission)) {
            throw new ForbiddenException(ErrorConstant.ADMIN_UNAUTHORIZED);
        }
    }

    /** Member-level transactions/balances: {@link AdminPermission#MEMBER_TRANSACTION_VIEW} (SUPER_ADMIN only). */
    public void requireMemberTransactionAccess() {
        requirePermission(AdminPermission.MEMBER_TRANSACTION_VIEW);
    }

    /** Same rule as {@link #requireMemberTransactionAccess()}, for masking fields instead of rejecting. */
    public boolean canViewMemberTransactions() {
        CustomAdminDetails admin = getCurrentAdmin();
        return RolePermissionMapper.hasPermission(admin.getRole(), AdminPermission.MEMBER_TRANSACTION_VIEW);
    }

    /**
     * Pages above {@link #BULK_PAGE_SIZE} are only requested by the portal export (Excel/CSV), so they
     * need {@link AdminPermission#REPORT_EXPORT} on top of the permission of the list itself.
     */
    public void requireExportIfBulk(int size) {
        if (size > BULK_PAGE_SIZE) {
            requirePermission(AdminPermission.REPORT_EXPORT);
        }
    }

    /** Permissions sent to the portal (menus, buttons): exactly the role's permissions. */
    public Set<AdminPermission> effectivePermissions(AdminRole role) {
        Set<AdminPermission> permissions = EnumSet.noneOf(AdminPermission.class);
        permissions.addAll(RolePermissionMapper.getPermissions(role));
        return permissions;
    }
}
