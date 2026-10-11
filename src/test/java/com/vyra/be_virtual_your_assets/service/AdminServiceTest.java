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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private AdminRepository adminRepository;
    @Mock private AdminTokenRepository adminTokenRepository;
    @Mock private AdminActivityService adminActivityService;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private Admin admin;
    private CustomAdminDetails currentAdmin;

    @BeforeEach
    void setUp() {
        admin = new Admin();
        admin.setId("admin-id-001");
        admin.setName("Budi Admin");
        admin.setEmail("budi@example.com");
        admin.setPhoneNumber("628111111111");
        admin.setPassword("hashed-password");
        admin.setRole(AdminRole.OPERATOR);
        admin.setStatus(ActiveStatus.ACTIVE);

        currentAdmin = new CustomAdminDetails("current-admin-id", "access-token-001", AdminRole.SUPER_ADMIN);
    }

    // =========================================================================
    // registerAdmin
    // =========================================================================
    @Test
    void registerAdmin_success_shouldSaveAndReturnResponse() {
        AdminRegisterRequest request = buildRegisterRequest();
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);
        when(adminRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(adminRepository.existsByPhoneNumber("628222222222")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-new-password");
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        BaseResponse<AdminRegisterResponse> result = adminService.registerAdmin(request, currentAdmin);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_REGISTER_SUCCESS.getCode());
        assertThat(result.getData().getEmail()).isEqualTo("new@example.com");
        assertThat(result.getData().getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        verify(adminRepository).save(any(Admin.class));
        verify(adminActivityService).createAdminActivity(eq("current-admin-id"), eq("current-admin-id"), eq(AdminActivityEvent.CREATE_ADMIN), any(), any());
    }

    @Test
    void registerAdmin_emailAlreadyExist_shouldThrowBusinessException() {
        AdminRegisterRequest request = buildRegisterRequest();
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);
        when(adminRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.registerAdmin(request, currentAdmin));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_EMAIL_ALREADY_EXIST);
        verify(adminRepository, never()).save(any());
    }

    @Test
    void registerAdmin_phoneAlreadyExist_shouldThrowBusinessException() {
        AdminRegisterRequest request = buildRegisterRequest();
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);
        when(adminRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(adminRepository.existsByPhoneNumber("628222222222")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.registerAdmin(request, currentAdmin));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_PHONE_ALREADY_EXIST);
        verify(adminRepository, never()).save(any());
    }

    @Test
    void registerAdmin_noPermission_shouldThrowForbiddenException() {
        AdminRegisterRequest request = buildRegisterRequest();
        doThrow(new ForbiddenException(ErrorConstant.ADMIN_UNAUTHORIZED))
                .when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);

        assertThrows(ForbiddenException.class, () -> adminService.registerAdmin(request, currentAdmin));
        verify(adminRepository, never()).save(any());
    }

    // =========================================================================
    // getAdminList
    // =========================================================================
    @Test
    void getAdminList_shouldReturnPagedList() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_VIEW);
        Page<Admin> page = new PageImpl<>(List.of(admin), PageRequest.of(0, 10), 1);
        when(adminRepository.searchAdmins(eq("budi"), any(PageRequest.class))).thenReturn(page);

        BaseResponse<PageResponse<AdminListItemResponse>> result = adminService.getAdminList("budi", 0, 10, currentAdmin);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_GET_LIST_SUCCESS.getCode());
        assertThat(result.getData().getContent()).hasSize(1);
        assertThat(result.getData().getContent().get(0).getEmail()).isEqualTo("budi@example.com");
    }

    @Test
    void getAdminList_blankKeyword_shouldPassEmptyStringToRepository() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_VIEW);
        Page<Admin> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(adminRepository.searchAdmins(eq(""), any(PageRequest.class))).thenReturn(page);

        adminService.getAdminList("   ", 0, 10, currentAdmin);

        verify(adminRepository).searchAdmins(eq(""), any(PageRequest.class));
    }

    // =========================================================================
    // getProfile
    // =========================================================================
    @Test
    void getProfile_found_shouldReturnProfileResponse() {
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));

        BaseResponse<AdminProfileResponse> result = adminService.getProfile("admin-id-001");

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_GET_PROFILE_SUCCESS.getCode());
        assertThat(result.getData().getEmail()).isEqualTo("budi@example.com");
        assertThat(result.getData().getPermissions()).isNotEmpty();
    }

    @Test
    void getProfile_notFound_shouldThrowAdminNotFound() {
        when(adminRepository.findById("unknown")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.getProfile("unknown"));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_NOT_FOUND);
    }

    // =========================================================================
    // getAdminDetail
    // =========================================================================
    @Test
    void getAdminDetail_found_shouldReturnProfileResponse() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_VIEW);
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));

        BaseResponse<AdminProfileResponse> result = adminService.getAdminDetail("admin-id-001", currentAdmin);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_GET_DETAIL_SUCCESS.getCode());
        assertThat(result.getData().getAdminId()).isEqualTo("admin-id-001");
    }

    // =========================================================================
    // updateAdmin
    // =========================================================================
    @Test
    void updateAdmin_success_shouldUpdateFieldsAndReturnResponse() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        AdminUpdateRequest request = new AdminUpdateRequest();
        request.setName("Budi Updated");
        request.setRole(AdminRole.ADMINISTRATOR);

        BaseResponse<AdminUpdateResponse> result = adminService.updateAdmin("admin-id-001", request, currentAdmin);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_UPDATE_SUCCESS.getCode());
        assertThat(result.getData().getName()).isEqualTo("Budi Updated");
        assertThat(result.getData().getRole()).isEqualTo(AdminRole.ADMINISTRATOR);
        assertThat(admin.getModifiedBy()).isEqualTo("current-admin-id");
    }

    @Test
    void updateAdmin_notFound_shouldThrowAdminNotFound() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);
        when(adminRepository.findById("unknown")).thenReturn(Optional.empty());

        AdminUpdateRequest request = new AdminUpdateRequest();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.updateAdmin("unknown", request, currentAdmin));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_NOT_FOUND);
    }

    // =========================================================================
    // updateOwnProfile
    // =========================================================================
    @Test
    void updateOwnProfile_success_shouldUpdateAllowedFieldsOnly() {
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        CustomAdminDetails self = new CustomAdminDetails("admin-id-001", "token", AdminRole.OPERATOR);
        AdminUpdateRequest request = new AdminUpdateRequest();
        request.setName("New Name");
        request.setRole(AdminRole.SUPER_ADMIN); // should be ignored by updateOwnProfile

        BaseResponse<AdminUpdateResponse> result = adminService.updateOwnProfile(request, self);

        assertThat(result.getData().getName()).isEqualTo("New Name");
        // role must remain unchanged - own profile update never escalates privilege
        assertThat(admin.getRole()).isEqualTo(AdminRole.OPERATOR);
        verify(adminActivityService).createAdminActivity(eq("admin-id-001"), anyString(), eq(AdminActivityEvent.UPDATE_PROFILE), any(), any());
    }

    // =========================================================================
    // updateAdminStatus
    // =========================================================================
    @Test
    void updateAdminStatus_success_shouldUpdateStatusAndRevokeTokensWhenNotActive() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminTokenRepository).deleteByAdminId("admin-id-001");
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        AdminUpdateStatusRequest request = new AdminUpdateStatusRequest();
        request.setStatus(ActiveStatus.SUSPENDED);

        BaseResponse<Void> result = adminService.updateAdminStatus("admin-id-001", request, currentAdmin);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_UPDATE_STATUS_SUCCESS.getCode());
        assertThat(admin.getStatus()).isEqualTo(ActiveStatus.SUSPENDED);
        verify(adminTokenRepository).deleteByAdminId("admin-id-001");
    }

    @Test
    void updateAdminStatus_settingActive_shouldNotRevokeTokens() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        AdminUpdateStatusRequest request = new AdminUpdateStatusRequest();
        request.setStatus(ActiveStatus.ACTIVE);

        adminService.updateAdminStatus("admin-id-001", request, currentAdmin);

        verify(adminTokenRepository, never()).deleteByAdminId(any());
    }

    @Test
    void updateAdminStatus_cannotModifySelf_shouldThrowForbiddenException() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);
        CustomAdminDetails self = new CustomAdminDetails("admin-id-001", "token", AdminRole.SUPER_ADMIN);

        AdminUpdateStatusRequest request = new AdminUpdateStatusRequest();
        request.setStatus(ActiveStatus.SUSPENDED);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> adminService.updateAdminStatus("admin-id-001", request, self));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_CANNOT_MODIFY_SELF_STATUS);
        verify(adminRepository, never()).save(any());
    }

    // =========================================================================
    // resetAdminPassword
    // =========================================================================
    @Test
    void resetAdminPassword_success_shouldEncodeAndRevokeTokens() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("hashed-new");
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminTokenRepository).deleteByAdminId("admin-id-001");
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        AdminResetPasswordRequest request = new AdminResetPasswordRequest();
        request.setNewPassword("NewPassword123!");

        BaseResponse<Void> result = adminService.resetAdminPassword("admin-id-001", request, currentAdmin);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_RESET_PASSWORD_SUCCESS.getCode());
        assertThat(admin.getPassword()).isEqualTo("hashed-new");
        verify(adminTokenRepository).deleteByAdminId("admin-id-001");
    }

    // =========================================================================
    // deleteAdmin
    // =========================================================================
    @Test
    void deleteAdmin_success_shouldSoftDeleteAndRevokeTokens() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminTokenRepository).deleteByAdminId("admin-id-001");
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        BaseResponse<Void> result = adminService.deleteAdmin("admin-id-001", currentAdmin);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_DELETE_SUCCESS.getCode());
        assertThat(admin.getStatus()).isEqualTo(ActiveStatus.DELETED);
        assertThat(admin.getDeletedAt()).isNotNull();
        verify(adminTokenRepository).deleteByAdminId("admin-id-001");
    }

    @Test
    void deleteAdmin_cannotDeleteSelf_shouldThrowForbiddenException() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);
        CustomAdminDetails self = new CustomAdminDetails("admin-id-001", "token", AdminRole.SUPER_ADMIN);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> adminService.deleteAdmin("admin-id-001", self));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_CANNOT_DELETE_SELF);
        verify(adminRepository, never()).save(any());
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private AdminRegisterRequest buildRegisterRequest() {
        AdminRegisterRequest request = new AdminRegisterRequest();
        request.setName("New Admin");
        request.setEmail("new@example.com");
        request.setPhoneNumber("628222222222");
        request.setRole(AdminRole.OPERATOR);
        request.setPassword("Password123!");
        return request;
    }


    // =========================================================================
    // profile field handling (shared by updateAdmin / updateOwnProfile)
    // =========================================================================
    @Test
    void updateOwnProfile_allFields_shouldTrimAndNormaliseEmail() {
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        AdminUpdateRequest request = new AdminUpdateRequest();
        request.setName("  New Name ");
        request.setEmail("  NEW@Example.COM ");
        request.setPhoneNumber("628111222333");

        adminService.updateOwnProfile(request, new CustomAdminDetails("admin-id-001", "token", AdminRole.OPERATOR));

        assertThat(admin.getName()).isEqualTo("New Name");
        assertThat(admin.getEmail()).isEqualTo("new@example.com");
        assertThat(admin.getPhoneNumber()).isEqualTo("628111222333");
    }

    @Test
    void updateOwnProfile_blankFields_shouldKeepExistingValues() {
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());
        String name = admin.getName();
        String email = admin.getEmail();
        String phone = admin.getPhoneNumber();

        AdminUpdateRequest request = new AdminUpdateRequest();
        request.setName("   ");
        request.setEmail("");
        request.setPhoneNumber(null);

        adminService.updateOwnProfile(request, new CustomAdminDetails("admin-id-001", "token", AdminRole.OPERATOR));

        assertThat(admin.getName()).isEqualTo(name);
        assertThat(admin.getEmail()).isEqualTo(email);
        assertThat(admin.getPhoneNumber()).isEqualTo(phone);
    }

    @Test
    void updateAdmin_emailPhoneAndNoRole_shouldApplyContactFieldsAndKeepRole() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.USER_MANAGE);
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());
        AdminRole roleBefore = admin.getRole();

        AdminUpdateRequest request = new AdminUpdateRequest();
        request.setEmail("Other@Example.com");
        request.setPhoneNumber("628000111222");

        adminService.updateAdmin("admin-id-001", request, currentAdmin);

        assertThat(admin.getEmail()).isEqualTo("other@example.com");
        assertThat(admin.getPhoneNumber()).isEqualTo("628000111222");
        assertThat(admin.getRole()).isEqualTo(roleBefore);
    }
}
