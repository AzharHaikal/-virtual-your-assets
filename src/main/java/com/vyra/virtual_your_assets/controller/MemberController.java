package com.vyra.virtual_your_assets.controller;

import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.register.ForgotPasswordRequest;
import com.vyra.virtual_your_assets.dto.register.ResetPasswordRequest;
import com.vyra.virtual_your_assets.service.MemberService;
import com.vyra.virtual_your_assets.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member")
public class MemberController {
    private final MemberService memberService;
    private final OtpService otpService;

    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(otpService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(memberService.resetPassword(request));
    }
}
