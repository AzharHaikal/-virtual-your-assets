package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.admin.*;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.*;

/**
 * Handles administrator account management (CRUD) and role-based access
 * control for the portal. Every endpoint here is protected by AdminTokenFilter
 * and gated by AdminAuthorizationService permission checks inside AdminService.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_ADMIN)
public class AdminController {

    private final AdminService adminService;

    @PostMapping(ADMIN_REGISTER)
    public BaseResponse<AdminRegisterResponse> registerAdmin(@Valid @RequestBody AdminRegisterRequest request, Authentication authentication) {
        CustomAdminDetails currentAdmin = (CustomAdminDetails) authentication.getPrincipal();
        return adminService.registerAdmin(request, currentAdmin);
    }

    @GetMapping(ADMIN_LIST)
    public BaseResponse<PageResponse<AdminListItemResponse>> getAdminList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        CustomAdminDetails currentAdmin = (CustomAdminDetails) authentication.getPrincipal();
        return adminService.getAdminList(keyword, page, size, currentAdmin);
    }

    @GetMapping(ADMIN_ME)
    public BaseResponse<AdminProfileResponse> getCurrentAdminProfile(Authentication authentication) {
        CustomAdminDetails currentAdmin = (CustomAdminDetails) authentication.getPrincipal();
        return adminService.getProfile(currentAdmin.getAdminId());
    }

    @GetMapping(ADMIN_DETAIL)
    public BaseResponse<AdminProfileResponse> getAdminDetail(@PathVariable String adminId, Authentication authentication) {
        CustomAdminDetails currentAdmin = (CustomAdminDetails) authentication.getPrincipal();
        return adminService.getAdminDetail(adminId, currentAdmin);
    }

    @PutMapping(ADMIN_UPDATE)
    public BaseResponse<AdminUpdateResponse> updateAdmin(
            @PathVariable String adminId,
            @Valid @RequestBody AdminUpdateRequest request,
            Authentication authentication
    ) {
        CustomAdminDetails currentAdmin = (CustomAdminDetails) authentication.getPrincipal();
        return adminService.updateAdmin(adminId, request, currentAdmin);
    }

    @PatchMapping(ADMIN_UPDATE_STATUS)
    public BaseResponse<Void> updateAdminStatus(
            @PathVariable String adminId,
            @Valid @RequestBody AdminUpdateStatusRequest request,
            Authentication authentication
    ) {
        CustomAdminDetails currentAdmin = (CustomAdminDetails) authentication.getPrincipal();
        return adminService.updateAdminStatus(adminId, request, currentAdmin);
    }

    @PatchMapping(ADMIN_RESET_PASSWORD)
    public BaseResponse<Void> resetAdminPassword(
            @PathVariable String adminId,
            @Valid @RequestBody AdminResetPasswordRequest request,
            Authentication authentication
    ) {
        CustomAdminDetails currentAdmin = (CustomAdminDetails) authentication.getPrincipal();
        return adminService.resetAdminPassword(adminId, request, currentAdmin);
    }

    @DeleteMapping(ADMIN_DELETE)
    public BaseResponse<Void> deleteAdmin(@PathVariable String adminId, Authentication authentication) {
        CustomAdminDetails currentAdmin = (CustomAdminDetails) authentication.getPrincipal();
        return adminService.deleteAdmin(adminId, currentAdmin);
    }

    @PutMapping(ADMIN_PROFILE)
    public BaseResponse<AdminUpdateResponse> updateOwnProfile(
            @Valid @RequestBody AdminUpdateRequest request,
            Authentication authentication
    ) {
        CustomAdminDetails currentAdmin = (CustomAdminDetails) authentication.getPrincipal();
        return adminService.updateOwnProfile(request, currentAdmin);
    }
}
