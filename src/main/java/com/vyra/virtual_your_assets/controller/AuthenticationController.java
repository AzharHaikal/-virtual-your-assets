package com.vyra.virtual_your_assets.controller;

import com.vyra.virtual_your_assets.constant.ApiPath;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.login.LoginRequest;
import com.vyra.virtual_your_assets.dto.login.LoginResponse;
import com.vyra.virtual_your_assets.dto.register.*;
import com.vyra.virtual_your_assets.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public BaseResponse<Void> resendOtp(@RequestBody @Valid ResendOtpRequest request) {
        return authenticationService.resendOtp(request);
    }

    @PostMapping(VERIFY_OTP)
    public BaseResponse<Void> verifyOtp(@RequestBody VerifyOtpRequest request) {
        return authenticationService.verifyOtp(request);
    }

    @PostMapping(LOGIN)
    public BaseResponse<LoginResponse> loginMember(@RequestBody LoginRequest request) {
        return authenticationService.loginMember(request);
    }

    @PostMapping(FORGOT_PIN)
    public BaseResponse<Void> forgotPin(@RequestBody ForgotPasswordRequest request) {
        return authenticationService.forgotPin(request);
    }

    @PostMapping(RESET_PIN)
    public BaseResponse<Void> resetPin(@RequestBody ResetPasswordRequest request) {
        return authenticationService.resetPin(request);
    }

}
