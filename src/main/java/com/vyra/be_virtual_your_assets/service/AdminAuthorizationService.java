package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.RolePermissionMapper;
import com.vyra.be_virtual_your_assets.constant.SystemConfigKey;
import com.vyra.be_virtual_your_assets.entity.SystemConfig;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import com.vyra.be_virtual_your_assets.repository.SystemConfigRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

/**
 * Centralized role-based access control (RBAC) helper for the admin module.
 * Every write/sensitive operation in AdminService and related services
 * should call {@link #requirePermission(AdminPermission)} before proceeding.
 * <p>
 * Member-level financial data additionally depends on a system setting: see
 * {@link #requireMemberTransactionAccess()}.
 */
@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private final SystemConfigRepository systemConfigRepository;

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

    /**
     * Member-level transactions/balances: the role must hold {@link AdminPermission#MEMBER_TRANSACTION_VIEW}
     * (SUPER_ADMIN only) and the setting {@link SystemConfigKey#MEMBER_TRANSACTION_DETAIL_ENABLED} must be on.
     */
    public void requireMemberTransactionAccess() {
        requirePermission(AdminPermission.MEMBER_TRANSACTION_VIEW);
        if (!isMemberTransactionDetailEnabled()) {
            throw new ForbiddenException(ErrorConstant.MEMBER_TRANSACTION_ACCESS_DISABLED);
        }
    }

    /** Same rule as {@link #requireMemberTransactionAccess()}, for masking fields instead of rejecting. */
    public boolean canViewMemberTransactions() {
        CustomAdminDetails admin = getCurrentAdmin();
        return RolePermissionMapper.hasPermission(admin.getRole(), AdminPermission.MEMBER_TRANSACTION_VIEW)
                && isMemberTransactionDetailEnabled();
    }

    /**
     * Permissions the portal should act on: the role's permissions, without
     * {@link AdminPermission#MEMBER_TRANSACTION_VIEW} while that access is switched off.
     */
    public Set<AdminPermission> effectivePermissions(AdminRole role) {
        Set<AdminPermission> permissions = EnumSet.noneOf(AdminPermission.class);
        permissions.addAll(RolePermissionMapper.getPermissions(role));
        if (!isMemberTransactionDetailEnabled()) {
            permissions.remove(AdminPermission.MEMBER_TRANSACTION_VIEW);
        }
        return permissions;
    }

    private boolean isMemberTransactionDetailEnabled() {
        SystemConfigKey key = SystemConfigKey.MEMBER_TRANSACTION_DETAIL_ENABLED;
        String value = systemConfigRepository.findById(key.getKey())
                .map(SystemConfig::getConfigValue)
                .orElse(key.getDefaultValue());
        return Boolean.parseBoolean(value);
    }
}
