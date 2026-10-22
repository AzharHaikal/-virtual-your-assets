package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.*;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.admin.*;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import com.vyra.be_virtual_your_assets.repository.AdminTokenRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages portal administrator accounts (CRUD) and enforces role-based access
 * control. This service is completely independent from anything related to
 * {@code Member} - it never reads from or writes to member tables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AdminRepository adminRepository;
    private final AdminTokenRepository adminTokenRepository;
    private final AdminActivityService adminActivityService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public BaseResponse<AdminRegisterResponse> registerAdmin(AdminRegisterRequest request, CustomAdminDetails currentAdmin) {
        adminAuthorizationService.requirePermission(AdminPermission.USER_MANAGE);
        log.info("[START] adminService.registerAdmin email: {}", request.getEmail());

        if (adminRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException(ErrorConstant.ADMIN_EMAIL_ALREADY_EXIST);
        }
        if (adminRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException(ErrorConstant.ADMIN_PHONE_ALREADY_EXIST);
        }

        Admin admin = new Admin();
        admin.setName(request.getName().trim());
        admin.setEmail(request.getEmail().toLowerCase().trim());
        admin.setPhoneNumber(request.getPhoneNumber());
        admin.setRole(request.getRole());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setStatus(ActiveStatus.ACTIVE);
        admin.setCreatedBy(currentAdmin.getAdminId());
        admin.setCreatedAt(LocalDateTime.now());
        adminRepository.save(admin);

        AdminRegisterResponse response = new AdminRegisterResponse();
        response.setAdminId(admin.getId());
        response.setName(admin.getName());
        response.setEmail(admin.getEmail());
        response.setPhoneNumber(admin.getPhoneNumber());
        response.setRole(admin.getRole());
        response.setStatus(admin.getStatus());

        adminActivityService.createAdminActivity(currentAdmin.getAdminId(), currentAdmin.getAdminId(), AdminActivityEvent.CREATE_ADMIN, null, null);
        log.info("[END] adminService.registerAdmin successfully email: {}", request.getEmail());

        return new BaseResponse<>(
                ErrorConstant.ADMIN_REGISTER_SUCCESS.getCode(),
                ErrorConstant.ADMIN_REGISTER_SUCCESS.getMessage(),
                response
        );
    }

    public BaseResponse<PageResponse<AdminListItemResponse>> getAdminList(String keyword, int page, int size, CustomAdminDetails currentAdmin) {
        adminAuthorizationService.requirePermission(AdminPermission.USER_VIEW);

        Page<Admin> adminPage = adminRepository.searchAdmins(
                StringUtils.isNotBlank(keyword) ? keyword.trim() : "",
                PageRequest.of(page, size)
        );

        List<AdminListItemResponse> items = adminPage.getContent()
                .stream()
                .map(this::toListItemResponse)
                .collect(Collectors.toList());

        return new BaseResponse<>(
                ErrorConstant.ADMIN_GET_LIST_SUCCESS.getCode(),
                ErrorConstant.ADMIN_GET_LIST_SUCCESS.getMessage(),
                PageResponse.from(adminPage, items)
        );
    }

    public BaseResponse<AdminProfileResponse> getProfile(String adminId) {
        Admin admin = getAdminOrThrow(adminId);
        return new BaseResponse<>(
                ErrorConstant.ADMIN_GET_PROFILE_SUCCESS.getCode(),
                ErrorConstant.ADMIN_GET_PROFILE_SUCCESS.getMessage(),
                toProfileResponse(admin)
        );
    }

    public BaseResponse<AdminProfileResponse> getAdminDetail(String adminId, CustomAdminDetails currentAdmin) {
        adminAuthorizationService.requirePermission(AdminPermission.USER_VIEW);
        Admin admin = getAdminOrThrow(adminId);
        return new BaseResponse<>(
                ErrorConstant.ADMIN_GET_DETAIL_SUCCESS.getCode(),
                ErrorConstant.ADMIN_GET_DETAIL_SUCCESS.getMessage(),
                toProfileResponse(admin)
        );
    }

    @Transactional
    public BaseResponse<AdminUpdateResponse> updateAdmin(String adminId, AdminUpdateRequest request, CustomAdminDetails currentAdmin) {
        adminAuthorizationService.requirePermission(AdminPermission.USER_MANAGE);
        Admin admin = getAdminOrThrow(adminId);

        applyUpdate(admin, request);
        admin.setModifiedBy(currentAdmin.getAdminId());
        admin.setUpdatedAt(LocalDateTime.now());
        adminRepository.save(admin);

        adminActivityService.createAdminActivity(currentAdmin.getAdminId(), currentAdmin.getAdminId(), AdminActivityEvent.UPDATE_ADMIN, null, null);

        return new BaseResponse<>(
                ErrorConstant.ADMIN_UPDATE_SUCCESS.getCode(),
                ErrorConstant.ADMIN_UPDATE_SUCCESS.getMessage(),
                toUpdateResponse(admin)
        );
    }

    @Transactional
    public BaseResponse<AdminUpdateResponse> updateOwnProfile(AdminUpdateRequest request, CustomAdminDetails currentAdmin) {
        Admin admin = getAdminOrThrow(currentAdmin.getAdminId());

        // Admins may update their own name/email/phone, but never their own role.
        applyProfileFields(admin, request);
        admin.setModifiedBy(currentAdmin.getAdminId());
        admin.setUpdatedAt(LocalDateTime.now());
        adminRepository.save(admin);

        adminActivityService.createAdminActivity(currentAdmin.getAdminId(), admin.getName(), AdminActivityEvent.UPDATE_PROFILE, null, null);

        return new BaseResponse<>(
                ErrorConstant.ADMIN_UPDATE_SUCCESS.getCode(),
                ErrorConstant.ADMIN_UPDATE_SUCCESS.getMessage(),
                toUpdateResponse(admin)
        );
    }

    @Transactional
    public BaseResponse<Void> updateAdminStatus(String adminId, AdminUpdateStatusRequest request, CustomAdminDetails currentAdmin) {
        adminAuthorizationService.requirePermission(AdminPermission.USER_MANAGE);

        if (adminId.equals(currentAdmin.getAdminId())) {
            throw new ForbiddenException(ErrorConstant.ADMIN_CANNOT_MODIFY_SELF_STATUS);
        }

        Admin admin = getAdminOrThrow(adminId);
        admin.setStatus(request.getStatus());
        if (request.getStatus() == ActiveStatus.ACTIVE) {
            // Re-activating also lifts a temporary login lock.
            admin.setFailedLoginAttempts(0);
            admin.setLockedUntil(null);
        }
        admin.setModifiedBy(currentAdmin.getAdminId());
        admin.setUpdatedAt(LocalDateTime.now());
        adminRepository.save(admin);

        // Revoke active sessions immediately if the admin is being deactivated/suspended.
        if (request.getStatus() != ActiveStatus.ACTIVE) {
            adminTokenRepository.deleteByAdminId(admin.getId());
        }

        adminActivityService.createAdminActivity(currentAdmin.getAdminId(), currentAdmin.getAdminId(), AdminActivityEvent.UPDATE_ADMIN_STATUS, null, null);

        return new BaseResponse<>(
                ErrorConstant.ADMIN_UPDATE_STATUS_SUCCESS.getCode(),
                ErrorConstant.ADMIN_UPDATE_STATUS_SUCCESS.getMessage(),
                null
        );
    }

    @Transactional
    public BaseResponse<Void> resetAdminPassword(String adminId, AdminResetPasswordRequest request, CustomAdminDetails currentAdmin) {
        adminAuthorizationService.requirePermission(AdminPermission.USER_MANAGE);

        Admin admin = getAdminOrThrow(adminId);
        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);
        admin.setModifiedBy(currentAdmin.getAdminId());
        admin.setUpdatedAt(LocalDateTime.now());
        adminRepository.save(admin);

        // Force re-login on all devices after a password reset.
        adminTokenRepository.deleteByAdminId(admin.getId());

        adminActivityService.createAdminActivity(currentAdmin.getAdminId(), currentAdmin.getAdminId(), AdminActivityEvent.RESET_ADMIN_PASSWORD, null, null);

        return new BaseResponse<>(
                ErrorConstant.ADMIN_RESET_PASSWORD_SUCCESS.getCode(),
                ErrorConstant.ADMIN_RESET_PASSWORD_SUCCESS.getMessage(),
                null
        );
    }

    @Transactional
    public BaseResponse<Void> deleteAdmin(String adminId, CustomAdminDetails currentAdmin) {
        adminAuthorizationService.requirePermission(AdminPermission.USER_MANAGE);

        if (adminId.equals(currentAdmin.getAdminId())) {
            throw new ForbiddenException(ErrorConstant.ADMIN_CANNOT_DELETE_SELF);
        }

        Admin admin = getAdminOrThrow(adminId);
        admin.setDeletedAt(LocalDateTime.now());
        admin.setStatus(ActiveStatus.DELETED);
        admin.setModifiedBy(currentAdmin.getAdminId());
        adminRepository.save(admin);

        adminTokenRepository.deleteByAdminId(admin.getId());

        adminActivityService.createAdminActivity(currentAdmin.getAdminId(), currentAdmin.getAdminId(), AdminActivityEvent.DELETE_ADMIN, null, null);

        return new BaseResponse<>(
                ErrorConstant.ADMIN_DELETE_SUCCESS.getCode(),
                ErrorConstant.ADMIN_DELETE_SUCCESS.getMessage(),
                null
        );
    }

    private Admin getAdminOrThrow(String adminId) {
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorConstant.ADMIN_NOT_FOUND));
    }

    /** Name / e-mail / phone: only non-blank values are applied. */
    private void applyProfileFields(Admin admin, AdminUpdateRequest request) {
        if (StringUtils.isNotBlank(request.getName())) {
            admin.setName(request.getName().trim());
        }
        if (StringUtils.isNotBlank(request.getEmail())) {
            admin.setEmail(request.getEmail().toLowerCase().trim());
        }
        if (StringUtils.isNotBlank(request.getPhoneNumber())) {
            admin.setPhoneNumber(request.getPhoneNumber());
        }
    }

    private void applyUpdate(Admin admin, AdminUpdateRequest request) {
        applyProfileFields(admin, request);
        if (request.getRole() != null) {
            admin.setRole(request.getRole());
        }
    }

    private AdminListItemResponse toListItemResponse(Admin admin) {
        AdminListItemResponse response = new AdminListItemResponse();
        response.setAdminId(admin.getId());
        response.setName(admin.getName());
        response.setEmail(admin.getEmail());
        response.setPhoneNumber(admin.getPhoneNumber());
        response.setRole(admin.getRole());
        response.setStatus(admin.getStatus());
        response.setLastLoginAt(admin.getLastLoginAt());
        return response;
    }

    private AdminUpdateResponse toUpdateResponse(Admin admin) {
        AdminUpdateResponse response = new AdminUpdateResponse();
        response.setAdminId(admin.getId());
        response.setName(admin.getName());
        response.setEmail(admin.getEmail());
        response.setPhoneNumber(admin.getPhoneNumber());
        response.setRole(admin.getRole());
        response.setStatus(admin.getStatus());
        return response;
    }

    private AdminProfileResponse toProfileResponse(Admin admin) {
        AdminProfileResponse response = new AdminProfileResponse();
        response.setAdminId(admin.getId());
        response.setName(admin.getName());
        response.setEmail(admin.getEmail());
        response.setPhoneNumber(admin.getPhoneNumber());
        response.setRole(admin.getRole());
        response.setStatus(admin.getStatus());
        response.setPermissions(
                RolePermissionMapper.getPermissions(admin.getRole())
                        .stream()
                        .map(AdminPermission::name)
                        .collect(Collectors.toSet())
        );
        response.setLastLoginAt(admin.getLastLoginAt());
        return response;
    }
}
