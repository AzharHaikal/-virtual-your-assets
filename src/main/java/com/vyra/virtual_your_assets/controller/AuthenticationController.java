package com.vyra.virtual_your_assets.controller;

import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.login.LoginRequest;
import com.vyra.virtual_your_assets.dto.login.LoginResponse;
import com.vyra.virtual_your_assets.dto.register.RegisterRequest;
import com.vyra.virtual_your_assets.dto.register.RegisterResponse;
import com.vyra.virtual_your_assets.dto.register.ResendOtpRequest;
import com.vyra.virtual_your_assets.dto.register.VerifyOtpRequest;
import com.vyra.virtual_your_assets.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<BaseResponse<Void>> resendOtp(@RequestBody @Valid ResendOtpRequest request) {
        return ResponseEntity.ok(authenticationService.resendOtp(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<BaseResponse<Void>> verifyOtp(@RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authenticationService.verifyOtp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

}
