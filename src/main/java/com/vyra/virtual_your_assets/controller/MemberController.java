package com.vyra.virtual_your_assets.controller;

import com.vyra.virtual_your_assets.constant.ApiPath;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.register.ForgotPasswordRequest;
import com.vyra.virtual_your_assets.dto.register.ResetPasswordRequest;
import com.vyra.virtual_your_assets.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static com.vyra.virtual_your_assets.constant.ApiPath.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_MEMBER)
public class MemberController {
    private final MemberService memberService;

    @PostMapping(FORGOT_PIN)
    public ResponseEntity<BaseResponse<Void>> forgotPin(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(memberService.forgotPin(request));
    }

    @PostMapping(RESET_PIN)
    public ResponseEntity<BaseResponse<Void>> resetPin(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(memberService.resetPin(request));
    }
}
