package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.*;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.admin.AdminLoginRequest;
import com.vyra.be_virtual_your_assets.dto.admin.AdminLoginResponse;
import com.vyra.be_virtual_your_assets.dto.admin.AdminRefreshTokenRequest;
import com.vyra.be_virtual_your_assets.dto.admin.AdminRefreshTokenResponse;
import com.vyra.be_virtual_your_assets.entity.Admin;
import com.vyra.be_virtual_your_assets.entity.AdminToken;
import com.vyra.be_virtual_your_assets.config.SecurityPolicy;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.repository.AdminRepository;
import com.vyra.be_virtual_your_assets.repository.AdminTokenRepository;
import com.vyra.be_virtual_your_assets.util.TokenUtil;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Authentication service for portal administrators ({@code Admin} entity, {@code admin_idp} schema).
 * Fully independent from {@code AuthenticationService}, which authenticates mobile app
 * members ({@code Member} entity, {@code idp} schema). The two never share tables, tokens,
 * or business rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthenticationService {

    private final AdminRepository adminRepository;
    private final AdminTokenRepository adminTokenRepository;
    private final AdminActivityService adminActivityService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecurityPolicy securityPolicy;

    @NewSpan
    @Transactional(noRollbackFor = {BusinessException.class, TooManyRequestsException.class})
    public BaseResponse<AdminLoginResponse> login(AdminLoginRequest request) {
        log.info("[START] adminAuthenticationService.login email: {}", request.getEmail());

        Admin admin = adminRepository.findByEmailIgnoreCase(request.getEmail()).orElse(null);

        if (admin == null) {
            log.info("Login failed, admin not found. email: {}", request.getEmail());
            createAdminActivity(null, request.getEmail(), AdminActivityEvent.FAILED_LOGIN, request.getIpAddress(), request.getDeviceName());
            throw new BusinessException(ErrorConstant.ADMIN_INVALID_CREDENTIALS);
        }

        if (admin.getStatus() != ActiveStatus.ACTIVE) {
            log.info("Login failed, admin not active. email: {}", request.getEmail());
            createAdminActivity(admin.getId(), admin.getName(), AdminActivityEvent.FAILED_LOGIN, request.getIpAddress(), request.getDeviceName());
            throw new BusinessException(ErrorConstant.ADMIN_NOT_ACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        if (admin.getLockedUntil() != null) {
            if (admin.getLockedUntil().isAfter(now)) {
                // Refused without checking the password, so a lock cannot be used as a guessing oracle.
                createAdminActivity(admin.getId(), admin.getName(), AdminActivityEvent.LOGIN_LOCKED, request.getIpAddress(), request.getDeviceName());
                throw new TooManyRequestsException(ErrorConstant.ACCOUNT_LOCKED,
                        Math.max(1, java.time.Duration.between(now, admin.getLockedUntil()).getSeconds()));
            }
            // Lock expired: start counting from zero again.
            admin.setLockedUntil(null);
            admin.setFailedLoginAttempts(0);
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            log.info("Login failed, invalid password. email: {}", request.getEmail());
            createAdminActivity(admin.getId(), admin.getName(), AdminActivityEvent.FAILED_LOGIN, request.getIpAddress(), request.getDeviceName());

            int attempts = (admin.getFailedLoginAttempts() == null ? 0 : admin.getFailedLoginAttempts()) + 1;
            if (attempts >= securityPolicy.getAdminMaxAttempts()) {
                admin.setFailedLoginAttempts(0);
                admin.setLockedUntil(now.plusMinutes(securityPolicy.getAdminLockMinutes()));
                adminRepository.save(admin);
                log.warn("Admin locked after {} failed logins. email: {}", attempts, request.getEmail());
                createAdminActivity(admin.getId(), admin.getName(), AdminActivityEvent.LOGIN_LOCKED, request.getIpAddress(), request.getDeviceName());
                throw new TooManyRequestsException(ErrorConstant.ACCOUNT_LOCKED, securityPolicy.getAdminLockMinutes() * 60L);
            }
            admin.setFailedLoginAttempts(attempts);
            adminRepository.save(admin);
            throw new BusinessException(ErrorConstant.ADMIN_INVALID_CREDENTIALS);
        }
        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);

        String accessToken = TokenUtil.generateAccessToken();
        String refreshToken = TokenUtil.generateRefreshToken();

        AdminToken adminToken = new AdminToken();
        adminToken.setAdminId(admin.getId());
        adminToken.setAccessToken(accessToken);
        adminToken.setRefreshToken(refreshToken);
        adminToken.setAccessTokenExpiredAt(TokenUtil.accessTokenExpiredAt());
        adminToken.setRefreshTokenExpiredAt(TokenUtil.refreshTokenExpiredAt());
        adminToken.setDeviceId(request.getDeviceId());
        adminToken.setDeviceName(request.getDeviceName());
        adminToken.setIpAddress(request.getIpAddress());
        adminToken.setCreatedAt(LocalDateTime.now());
        adminTokenRepository.save(adminToken);

        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        AdminLoginResponse response = new AdminLoginResponse();
        response.setAdminId(admin.getId());
        response.setName(admin.getName());
        response.setEmail(admin.getEmail());
        response.setRole(admin.getRole());
        response.setPermissions(
                RolePermissionMapper.getPermissions(admin.getRole())
                        .stream()
                        .map(AdminPermission::name)
                        .collect(Collectors.toSet())
        );
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setAccessTokenExpiredAt(adminToken.getAccessTokenExpiredAt());
        response.setRefreshTokenExpiredAt(adminToken.getRefreshTokenExpiredAt());

        createAdminActivity(admin.getId(), admin.getName(), AdminActivityEvent.SUCCESS_LOGIN, request.getIpAddress(), request.getDeviceName());
        log.info("[END] adminAuthenticationService.login successfully email: {}", request.getEmail());

        return new BaseResponse<>(
                ErrorConstant.ADMIN_LOGIN_SUCCESS.getCode(),
                ErrorConstant.ADMIN_LOGIN_SUCCESS.getMessage(),
                response
        );
    }

    @NewSpan
    @Transactional(noRollbackFor = BusinessException.class)
    public BaseResponse<AdminRefreshTokenResponse> refreshToken(AdminRefreshTokenRequest request) {
        AdminToken adminToken = adminTokenRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException(ErrorConstant.ADMIN_INVALID_REFRESH_TOKEN));

        if (adminToken.getRefreshTokenExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorConstant.ADMIN_REFRESH_TOKEN_EXPIRED);
        }

        // A suspended / deleted admin cannot renew a session; the dead token is removed.
        Admin tokenOwner = adminRepository.findById(adminToken.getAdminId()).orElse(null);
        if (tokenOwner == null || tokenOwner.getStatus() != ActiveStatus.ACTIVE) {
            adminTokenRepository.delete(adminToken);
            throw new BusinessException(ErrorConstant.ADMIN_NOT_ACTIVE);
        }

        String newAccessToken = TokenUtil.generateAccessToken();
        String newRefreshToken = TokenUtil.generateRefreshToken();

        adminToken.setAccessToken(newAccessToken);
        adminToken.setRefreshToken(newRefreshToken);
        adminToken.setAccessTokenExpiredAt(TokenUtil.accessTokenExpiredAt());
        adminToken.setRefreshTokenExpiredAt(TokenUtil.refreshTokenExpiredAt());
        adminTokenRepository.save(adminToken);

        AdminRefreshTokenResponse response = new AdminRefreshTokenResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setAccessTokenExpiredAt(adminToken.getAccessTokenExpiredAt());
        response.setRefreshTokenExpiredAt(adminToken.getRefreshTokenExpiredAt());

        return new BaseResponse<>(
                ErrorConstant.ADMIN_REFRESH_TOKEN_SUCCESS.getCode(),
                ErrorConstant.ADMIN_REFRESH_TOKEN_SUCCESS.getMessage(),
                response
        );
    }

    @NewSpan
    @Transactional
    public BaseResponse<Void> logout(String adminId, String accessToken) {
        Admin admin = adminRepository.findById(adminId).orElse(null);
        adminTokenRepository.deleteByAccessToken(accessToken);

        if (admin != null) {
            createAdminActivity(admin.getId(), admin.getName(), AdminActivityEvent.SUCCESS_LOGOUT, null, null);
        }

        return new BaseResponse<>(
                ErrorConstant.ADMIN_LOGOUT_SUCCESS.getCode(),
                ErrorConstant.ADMIN_LOGOUT_SUCCESS.getMessage(),
                null
        );
    }

    private void createAdminActivity(String adminId, String adminName, AdminActivityEvent event, String ipAddress, String device) {
        adminActivityService.createAdminActivity(adminId, adminName, event, ipAddress, device);
    }
}
