package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.auth.ChangePinRequest;
import com.vyra.be_virtual_your_assets.dto.member.GetMemberResponse;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileRequest;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.MemberService;
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
class MemberControllerTest {

    @Mock
    private MemberService memberService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MemberController memberController;

    private final String MEMBER_ID = "member-uuid-002";
    private final String ACCESS_TOKEN = "access-token-xyz";

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new CustomUserDetails(MEMBER_ID, ACCESS_TOKEN);
    }

    // =========================================================================
    // getMember
    // =========================================================================
    @Test
    void getMember_shouldExtractPrincipalAndDelegateToService() {
        GetMemberResponse data = new GetMemberResponse();
        BaseResponse<GetMemberResponse> expected = new BaseResponse<>("VYRA-GMS-000", "Success", data);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(memberService.getMember(MEMBER_ID)).thenReturn(expected);

        BaseResponse<GetMemberResponse> result = memberController.getMember(authentication);

        assertThat(result).isSameAs(expected);
        verify(authentication).getPrincipal();
        verify(memberService).getMember(MEMBER_ID);
    }

    // =========================================================================
    // updateProfile
    // =========================================================================
    @Test
    void updateProfile_shouldExtractPrincipalAndDelegateToService() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        UpdateProfileResponse data = new UpdateProfileResponse();
        BaseResponse<UpdateProfileResponse> expected = new BaseResponse<>("VYRA-UPS-000", "Updated", data);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(memberService.updateProfile(MEMBER_ID, request)).thenReturn(expected);

        BaseResponse<UpdateProfileResponse> result = memberController.updateProfile(authentication, request);

        assertThat(result).isSameAs(expected);
        verify(authentication).getPrincipal();
        verify(memberService).updateProfile(MEMBER_ID, request);
    }

    // =========================================================================
    // changePin (mapped to PATCH /change-pin, method name 'updateProfile' overload)
    // =========================================================================
    @Test
    void changePin_shouldExtractPrincipalAndDelegateToService() {
        ChangePinRequest request = new ChangePinRequest();
        BaseResponse<Void> expected = new BaseResponse<>("VYRA-CPS-000", "PIN changed", null);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(memberService.changePin(MEMBER_ID, request)).thenReturn(expected);

        BaseResponse<Void> result = memberController.updateProfile(authentication, request);

        assertThat(result).isSameAs(expected);
        verify(authentication).getPrincipal();
        verify(memberService).changePin(MEMBER_ID, request);
    }
}
