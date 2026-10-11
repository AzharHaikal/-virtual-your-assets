package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.admin.AdminLoginRequest;
import com.vyra.be_virtual_your_assets.dto.admin.AdminLoginResponse;
import com.vyra.be_virtual_your_assets.dto.admin.AdminRefreshTokenRequest;
import com.vyra.be_virtual_your_assets.dto.admin.AdminRefreshTokenResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.service.AdminAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthenticationControllerTest {

    @Mock private AdminAuthenticationService adminAuthenticationService;
    @Mock private Authentication authentication;
    @Mock private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AdminAuthenticationController adminAuthenticationController;

    private CustomAdminDetails adminDetails;

    @BeforeEach
    void setUp() {
        adminDetails = new CustomAdminDetails("admin-id-001", "access-token-xyz", AdminRole.SUPER_ADMIN);
    }

    // =========================================================================
    // login
    // =========================================================================
    @Test
    void login_ipAddressProvided_shouldNotOverrideWithRemoteAddr() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("Secret123!");
        request.setIpAddress("10.0.0.1");

        BaseResponse<AdminLoginResponse> expected = new BaseResponse<>("VYRA-ADM-LGN-000", "Success", new AdminLoginResponse());
        when(adminAuthenticationService.login(request)).thenReturn(expected);

        BaseResponse<AdminLoginResponse> result = adminAuthenticationController.login(request, httpServletRequest);

        assertThat(result).isSameAs(expected);
        assertThat(request.getIpAddress()).isEqualTo("10.0.0.1");
        verify(httpServletRequest, never()).getRemoteAddr();
    }

    @Test
    void login_ipAddressBlank_shouldFallBackToRemoteAddr() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("Secret123!");
        request.setIpAddress("");
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.50");

        BaseResponse<AdminLoginResponse> expected = new BaseResponse<>("VYRA-ADM-LGN-000", "Success", new AdminLoginResponse());
        when(adminAuthenticationService.login(request)).thenReturn(expected);

        adminAuthenticationController.login(request, httpServletRequest);

        assertThat(request.getIpAddress()).isEqualTo("192.168.1.50");
    }

    @Test
    void login_ipAddressNull_shouldFallBackToRemoteAddr() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("Secret123!");
        when(httpServletRequest.getRemoteAddr()).thenReturn("172.16.0.5");

        BaseResponse<AdminLoginResponse> expected = new BaseResponse<>("VYRA-ADM-LGN-000", "Success", new AdminLoginResponse());
        when(adminAuthenticationService.login(request)).thenReturn(expected);

        adminAuthenticationController.login(request, httpServletRequest);

        assertThat(request.getIpAddress()).isEqualTo("172.16.0.5");
    }

    // =========================================================================
    // refreshToken
    // =========================================================================
    @Test
    void refreshToken_shouldDelegateToService() {
        AdminRefreshTokenRequest request = new AdminRefreshTokenRequest();
        request.setRefreshToken("refresh-token-001");

        BaseResponse<AdminRefreshTokenResponse> expected = new BaseResponse<>("VYRA-ADM-TKN-000", "Success", new AdminRefreshTokenResponse());
        when(adminAuthenticationService.refreshToken(request)).thenReturn(expected);

        BaseResponse<AdminRefreshTokenResponse> result = adminAuthenticationController.refreshToken(request);

        assertThat(result).isSameAs(expected);
        verify(adminAuthenticationService).refreshToken(request);
    }

    // =========================================================================
    // logout
    // =========================================================================
    @Test
    void logout_shouldExtractPrincipalAndDelegateToService() {
        when(authentication.getPrincipal()).thenReturn(adminDetails);
        BaseResponse<Void> expected = new BaseResponse<>("VYRA-ADM-LGT-000", "Success", null);
        when(adminAuthenticationService.logout("admin-id-001", "access-token-xyz")).thenReturn(expected);

        BaseResponse<Void> result = adminAuthenticationController.logout(authentication);

        assertThat(result).isSameAs(expected);
        verify(adminAuthenticationService).logout("admin-id-001", "access-token-xyz");
    }
}
