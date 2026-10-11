package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.admin.AdminLoginRequest;
import com.vyra.be_virtual_your_assets.dto.admin.AdminLoginResponse;
import com.vyra.be_virtual_your_assets.dto.admin.AdminRefreshTokenRequest;
import com.vyra.be_virtual_your_assets.dto.admin.AdminRefreshTokenResponse;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.entity.AdminToken;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import com.vyra.be_virtual_your_assets.repository.AdminTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthenticationServiceTest {

    @Mock private AdminRepository adminRepository;
    @Mock private AdminTokenRepository adminTokenRepository;
    @Mock private AdminActivityService adminActivityService;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminAuthenticationService adminAuthenticationService;

    private Admin admin;

    @BeforeEach
    void setUp() {
        admin = new Admin();
        admin.setId("admin-id-001");
        admin.setName("Budi Admin");
        admin.setEmail("admin@example.com");
        admin.setPhoneNumber("628111111111");
        admin.setPassword("hashed-password");
        admin.setRole(AdminRole.SUPER_ADMIN);
        admin.setStatus(ActiveStatus.ACTIVE);
    }

    // =========================================================================
    // login — success
    // =========================================================================
    @Test
    void login_success_shouldSaveTokenAndReturnLoginResponse() {
        AdminLoginRequest request = buildLoginRequest();
        when(adminRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Secret123!", "hashed-password")).thenReturn(true);
        when(adminTokenRepository.save(any(AdminToken.class))).thenReturn(new AdminToken());
        doNothing().when(adminActivityService).createAdminActivity(anyString(), anyString(), any(), any(), any());

        BaseResponse<AdminLoginResponse> result = adminAuthenticationService.login(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_LOGIN_SUCCESS.getCode());
        assertThat(result.getData().getEmail()).isEqualTo("admin@example.com");
        assertThat(result.getData().getRole()).isEqualTo(AdminRole.SUPER_ADMIN);
        assertThat(result.getData().getAccessToken()).isNotBlank();
        assertThat(result.getData().getRefreshToken()).isNotBlank();
        assertThat(result.getData().getPermissions()).isNotEmpty();
        verify(adminTokenRepository).save(any(AdminToken.class));
        verify(adminRepository).save(admin);
        verify(adminActivityService).createAdminActivity(eq("admin-id-001"), eq("Budi Admin"), eq(AdminActivityEvent.SUCCESS_LOGIN), any(), any());
    }

    // =========================================================================
    // login — admin not found
    // =========================================================================
    @Test
    void login_adminNotFound_shouldThrowInvalidCredentials() {
        AdminLoginRequest request = buildLoginRequest();
        when(adminRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminAuthenticationService.login(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_INVALID_CREDENTIALS);
        verify(adminTokenRepository, never()).save(any());
        verify(adminActivityService).createAdminActivity(isNull(), eq("admin@example.com"), eq(AdminActivityEvent.FAILED_LOGIN), any(), any());
    }

    // =========================================================================
    // login — admin not active
    // =========================================================================
    @Test
    void login_adminNotActive_shouldThrowAdminNotActive() {
        admin.setStatus(ActiveStatus.SUSPENDED);
        AdminLoginRequest request = buildLoginRequest();
        when(adminRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminAuthenticationService.login(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_NOT_ACTIVE);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    // =========================================================================
    // login — wrong password
    // =========================================================================
    @Test
    void login_wrongPassword_shouldThrowInvalidCredentials() {
        AdminLoginRequest request = buildLoginRequest();
        when(adminRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Secret123!", "hashed-password")).thenReturn(false);
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminAuthenticationService.login(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_INVALID_CREDENTIALS);
        verify(adminTokenRepository, never()).save(any());
    }

    // =========================================================================
    // refreshToken — success
    // =========================================================================
    @Test
    void refreshToken_success_shouldReturnNewTokens() {
        AdminRefreshTokenRequest request = new AdminRefreshTokenRequest();
        request.setRefreshToken("old-refresh-token");

        AdminToken existingToken = new AdminToken();
        existingToken.setAdminId("admin-id-001");
        existingToken.setAccessToken("old-access-token");
        existingToken.setRefreshToken("old-refresh-token");
        existingToken.setRefreshTokenExpiredAt(LocalDateTime.now().plusDays(10));

        when(adminTokenRepository.findByRefreshToken("old-refresh-token")).thenReturn(Optional.of(existingToken));
        when(adminTokenRepository.save(any(AdminToken.class))).thenReturn(existingToken);

        BaseResponse<AdminRefreshTokenResponse> result = adminAuthenticationService.refreshToken(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_REFRESH_TOKEN_SUCCESS.getCode());
        assertThat(result.getData().getAccessToken()).isNotBlank();
        assertThat(result.getData().getAccessToken()).isNotEqualTo("old-access-token");
        verify(adminTokenRepository).save(existingToken);
    }

    // =========================================================================
    // refreshToken — token not found
    // =========================================================================
    @Test
    void refreshToken_notFound_shouldThrowInvalidRefreshToken() {
        AdminRefreshTokenRequest request = new AdminRefreshTokenRequest();
        request.setRefreshToken("unknown-token");
        when(adminTokenRepository.findByRefreshToken("unknown-token")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminAuthenticationService.refreshToken(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_INVALID_REFRESH_TOKEN);
    }

    // =========================================================================
    // refreshToken — token expired
    // =========================================================================
    @Test
    void refreshToken_expired_shouldThrowRefreshTokenExpired() {
        AdminRefreshTokenRequest request = new AdminRefreshTokenRequest();
        request.setRefreshToken("expired-token");

        AdminToken expiredToken = new AdminToken();
        expiredToken.setRefreshToken("expired-token");
        expiredToken.setRefreshTokenExpiredAt(LocalDateTime.now().minusMinutes(1));

        when(adminTokenRepository.findByRefreshToken("expired-token")).thenReturn(Optional.of(expiredToken));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminAuthenticationService.refreshToken(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ADMIN_REFRESH_TOKEN_EXPIRED);
        verify(adminTokenRepository, never()).save(any());
    }

    // =========================================================================
    // logout — success, admin found
    // =========================================================================
    @Test
    void logout_adminFound_shouldDeleteTokenAndLogActivity() {
        when(adminRepository.findById("admin-id-001")).thenReturn(Optional.of(admin));
        doNothing().when(adminTokenRepository).deleteByAccessToken("access-token-001");
        doNothing().when(adminActivityService).createAdminActivity(any(), any(), any(), any(), any());

        BaseResponse<Void> result = adminAuthenticationService.logout("admin-id-001", "access-token-001");

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_LOGOUT_SUCCESS.getCode());
        verify(adminTokenRepository).deleteByAccessToken("access-token-001");
        verify(adminActivityService).createAdminActivity(eq("admin-id-001"), eq("Budi Admin"), eq(AdminActivityEvent.SUCCESS_LOGOUT), any(), any());
    }

    // =========================================================================
    // logout — admin not found, should still delete token without logging activity
    // =========================================================================
    @Test
    void logout_adminNotFound_shouldStillDeleteTokenWithoutLoggingActivity() {
        when(adminRepository.findById("unknown-admin")).thenReturn(Optional.empty());
        doNothing().when(adminTokenRepository).deleteByAccessToken("access-token-002");

        BaseResponse<Void> result = adminAuthenticationService.logout("unknown-admin", "access-token-002");

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ADMIN_LOGOUT_SUCCESS.getCode());
        verify(adminTokenRepository).deleteByAccessToken("access-token-002");
        verify(adminActivityService, never()).createAdminActivity(any(), any(), any(), any(), any());
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private AdminLoginRequest buildLoginRequest() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("Secret123!");
        request.setDeviceId("device-001");
        request.setDeviceName("Chrome on Windows");
        request.setIpAddress("127.0.0.1");
        return request;
    }
}
