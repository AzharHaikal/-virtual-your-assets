package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.activity.MemberActivityResponse;
import com.vyra.be_virtual_your_assets.dto.auth.ChangePinRequest;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.member.GetMemberResponse;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileRequest;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileResponse;
import com.vyra.be_virtual_your_assets.dto.member.VerifyContactChangeRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_MEMBER)
public class MemberController {
    private final MemberService memberService;

    @GetMapping(GET_MEMBER)
    public BaseResponse<GetMemberResponse> getMember(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return memberService.getMember(user.getMemberId());
    }

    @GetMapping(GET_MEMBER_ACTIVITY)
    public BaseResponse<PageResponse<MemberActivityResponse>> getMemberActivity(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return memberService.getMemberActivity(user.getMemberId(), page, size);
    }

    @PatchMapping(UPDATE_PROFILE)
    public BaseResponse<UpdateProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return memberService.updateProfile(user.getMemberId(), request);
    }

    @PostMapping(UPDATE_PROFILE_VERIFY)
    public BaseResponse<UpdateProfileResponse> verifyContactChange(
            Authentication authentication,
            @Valid @RequestBody VerifyContactChangeRequest request
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return memberService.verifyContactChange(user.getMemberId(), request, user.getAccessToken());
    }

    @PostMapping(UPDATE_PROFILE_RESEND_OTP)
    public BaseResponse<UpdateProfileResponse> resendContactChangeOtp(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return memberService.resendContactChangeOtp(user.getMemberId());
    }

    @PatchMapping(CHANGE_PIN)
    public BaseResponse<Void> updateProfile(
            Authentication authentication,
            @Valid @RequestBody ChangePinRequest request
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return memberService.changePin(user.getMemberId(), request, user.getAccessToken());
    }
}
