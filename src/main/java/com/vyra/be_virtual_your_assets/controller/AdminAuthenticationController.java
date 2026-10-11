package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.admin.AdminLoginRequest;
import com.vyra.be_virtual_your_assets.dto.admin.AdminLoginResponse;
import com.vyra.be_virtual_your_assets.dto.admin.AdminRefreshTokenRequest;
import com.vyra.be_virtual_your_assets.dto.admin.AdminRefreshTokenResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.service.AdminAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.*;

/**
 * Handles authentication for portal administrators only. This controller and
 * its underlying service are fully independent from {@code AuthenticationController},
 * which authenticates mobile app members. Admins and members never share a token type.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_ADMIN_AUTH)
public class AdminAuthenticationController {

    private final AdminAuthenticationService adminAuthenticationService;

    @PostMapping(ADMIN_LOGIN)
    public BaseResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request, HttpServletRequest httpServletRequest) {
        if (request.getIpAddress() == null || request.getIpAddress().isBlank()) {
            request.setIpAddress(httpServletRequest.getRemoteAddr());
        }
        return adminAuthenticationService.login(request);
    }

    @PostMapping(ADMIN_REFRESH_TOKEN)
    public BaseResponse<AdminRefreshTokenResponse> refreshToken(@Valid @RequestBody AdminRefreshTokenRequest request) {
        return adminAuthenticationService.refreshToken(request);
    }

    @PostMapping(ADMIN_LOGOUT)
    public BaseResponse<Void> logout(Authentication authentication) {
        CustomAdminDetails admin = (CustomAdminDetails) authentication.getPrincipal();
        return adminAuthenticationService.logout(admin.getAdminId(), admin.getAccessToken());
    }
}
