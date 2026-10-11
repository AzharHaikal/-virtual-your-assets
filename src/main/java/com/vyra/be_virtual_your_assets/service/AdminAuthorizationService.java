package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.RolePermissionMapper;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Centralized role-based access control (RBAC) helper for the admin module.
 * Every write/sensitive operation in AdminService and related services
 * should call {@link #requirePermission(AdminPermission)} before proceeding.
 */
@Service
public class AdminAuthorizationService {

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
}
