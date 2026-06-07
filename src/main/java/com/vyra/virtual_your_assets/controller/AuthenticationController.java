package com.vyra.virtual_your_assets.controller;

import com.vyra.virtual_your_assets.constant.ApiPath;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.auth.*;
import com.vyra.virtual_your_assets.dto.member.UpdateProfileRequest;
import com.vyra.virtual_your_assets.dto.member.UpdateProfileResponse;
import com.vyra.virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.virtual_your_assets.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.vyra.virtual_your_assets.constant.ApiPath.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_AUTH)
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping(REGISTER)
    public BaseResponse<RegisterResponse> registerMember(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.registerMember(request);
    }

    @PostMapping(RESEND_OTP)
    public BaseResponse<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return authenticationService.resendOtp(request);
    }

    @PostMapping(VERIFY_OTP)
    public BaseResponse<Void> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authenticationService.verifyOtp(request);
    }

    @PostMapping(LOGIN)
    public BaseResponse<LoginResponse> loginMember(@Valid @RequestBody LoginRequest request) {
        return authenticationService.loginMember(request);
    }

    @PostMapping(REFRESH_TOKEN)
    public BaseResponse<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authenticationService.refreshToken(request);
    }

    @PostMapping(FORGOT_PIN)
    public BaseResponse<Void> forgotPin(@Valid @RequestBody ForgotPinRequest request) {
        return authenticationService.forgotPin(request);
    }

    @PostMapping(RESET_PIN)
    public BaseResponse<Void> resetPin(@RequestBody ResetPinRequest request) {
        return authenticationService.resetPin(request);
    }

    @PostMapping(LOGOUT)
    public BaseResponse<Void> logoutMember(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return authenticationService.logoutMember(user.getMemberId(), user.getAccessToken());
    }

}
