package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.admin.*;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private AdminService adminService;
    @Mock private Authentication authentication;

    @InjectMocks
    private AdminController adminController;

    private CustomAdminDetails currentAdmin;

    @BeforeEach
    void setUp() {
        currentAdmin = new CustomAdminDetails("current-admin-id", "token", AdminRole.SUPER_ADMIN);
    }

    @Test
    void registerAdmin_shouldExtractPrincipalAndDelegateToService() {
        AdminRegisterRequest request = new AdminRegisterRequest();
        when(authentication.getPrincipal()).thenReturn(currentAdmin);
        BaseResponse<AdminRegisterResponse> expected = new BaseResponse<>("VYRA-ADM-REG-000", "Success", new AdminRegisterResponse());
        when(adminService.registerAdmin(request, currentAdmin)).thenReturn(expected);

        BaseResponse<AdminRegisterResponse> result = adminController.registerAdmin(request, authentication);

        assertThat(result).isSameAs(expected);
        verify(adminService).registerAdmin(request, currentAdmin);
    }

    @Test
    void getAdminList_shouldExtractPrincipalAndDelegateToService() {
        when(authentication.getPrincipal()).thenReturn(currentAdmin);
        BaseResponse<PageResponse<AdminListItemResponse>> expected = new BaseResponse<>("VYRA-ADM-LST-000", "Success", null);
        when(adminService.getAdminList("budi", 0, 10, currentAdmin)).thenReturn(expected);

        BaseResponse<PageResponse<AdminListItemResponse>> result = adminController.getAdminList("budi", 0, 10, authentication);

        assertThat(result).isSameAs(expected);
        verify(adminService).getAdminList("budi", 0, 10, currentAdmin);
    }

    @Test
    void getCurrentAdminProfile_shouldExtractPrincipalAndDelegateToService() {
        when(authentication.getPrincipal()).thenReturn(currentAdmin);
        BaseResponse<AdminProfileResponse> expected = new BaseResponse<>("VYRA-ADM-PRF-000", "Success", new AdminProfileResponse());
        when(adminService.getProfile("current-admin-id")).thenReturn(expected);

        BaseResponse<AdminProfileResponse> result = adminController.getCurrentAdminProfile(authentication);

        assertThat(result).isSameAs(expected);
        verify(adminService).getProfile("current-admin-id");
    }

    @Test
    void getAdminDetail_shouldExtractPrincipalAndDelegateToService() {
        when(authentication.getPrincipal()).thenReturn(currentAdmin);
        BaseResponse<AdminProfileResponse> expected = new BaseResponse<>("VYRA-ADM-DTL-000", "Success", new AdminProfileResponse());
        when(adminService.getAdminDetail("target-admin-id", currentAdmin)).thenReturn(expected);

        BaseResponse<AdminProfileResponse> result = adminController.getAdminDetail("target-admin-id", authentication);

        assertThat(result).isSameAs(expected);
        verify(adminService).getAdminDetail("target-admin-id", currentAdmin);
    }

    @Test
    void updateAdmin_shouldExtractPrincipalAndDelegateToService() {
        AdminUpdateRequest request = new AdminUpdateRequest();
        when(authentication.getPrincipal()).thenReturn(currentAdmin);
        BaseResponse<AdminUpdateResponse> expected = new BaseResponse<>("VYRA-ADM-UPD-000", "Success", new AdminUpdateResponse());
        when(adminService.updateAdmin("target-admin-id", request, currentAdmin)).thenReturn(expected);

        BaseResponse<AdminUpdateResponse> result = adminController.updateAdmin("target-admin-id", request, authentication);

        assertThat(result).isSameAs(expected);
        verify(adminService).updateAdmin("target-admin-id", request, currentAdmin);
    }

    @Test
    void updateAdminStatus_shouldExtractPrincipalAndDelegateToService() {
        AdminUpdateStatusRequest request = new AdminUpdateStatusRequest();
        when(authentication.getPrincipal()).thenReturn(currentAdmin);
        BaseResponse<Void> expected = new BaseResponse<>("VYRA-ADM-STS-000", "Success", null);
        when(adminService.updateAdminStatus("target-admin-id", request, currentAdmin)).thenReturn(expected);

        BaseResponse<Void> result = adminController.updateAdminStatus("target-admin-id", request, authentication);

        assertThat(result).isSameAs(expected);
        verify(adminService).updateAdminStatus("target-admin-id", request, currentAdmin);
    }

    @Test
    void resetAdminPassword_shouldExtractPrincipalAndDelegateToService() {
        AdminResetPasswordRequest request = new AdminResetPasswordRequest();
        when(authentication.getPrincipal()).thenReturn(currentAdmin);
        BaseResponse<Void> expected = new BaseResponse<>("VYRA-ADM-RPW-000", "Success", null);
        when(adminService.resetAdminPassword("target-admin-id", request, currentAdmin)).thenReturn(expected);

        BaseResponse<Void> result = adminController.resetAdminPassword("target-admin-id", request, authentication);

        assertThat(result).isSameAs(expected);
        verify(adminService).resetAdminPassword("target-admin-id", request, currentAdmin);
    }

    @Test
    void deleteAdmin_shouldExtractPrincipalAndDelegateToService() {
        when(authentication.getPrincipal()).thenReturn(currentAdmin);
        BaseResponse<Void> expected = new BaseResponse<>("VYRA-ADM-DEL-000", "Success", null);
        when(adminService.deleteAdmin("target-admin-id", currentAdmin)).thenReturn(expected);

        BaseResponse<Void> result = adminController.deleteAdmin("target-admin-id", authentication);

        assertThat(result).isSameAs(expected);
        verify(adminService).deleteAdmin("target-admin-id", currentAdmin);
    }

    @Test
    void updateOwnProfile_shouldExtractPrincipalAndDelegateToService() {
        AdminUpdateRequest request = new AdminUpdateRequest();
        when(authentication.getPrincipal()).thenReturn(currentAdmin);
        BaseResponse<AdminUpdateResponse> expected = new BaseResponse<>("VYRA-ADM-UPD-000", "Success", new AdminUpdateResponse());
        when(adminService.updateOwnProfile(request, currentAdmin)).thenReturn(expected);

        BaseResponse<AdminUpdateResponse> result = adminController.updateOwnProfile(request, authentication);

        assertThat(result).isSameAs(expected);
        verify(adminService).updateOwnProfile(request, currentAdmin);
    }
}
