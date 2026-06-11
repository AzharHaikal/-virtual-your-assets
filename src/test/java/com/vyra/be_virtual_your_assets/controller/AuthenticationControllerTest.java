package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.auth.*;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.AuthenticationService;
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
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticationController authenticationController;

    private final String MEMBER_ID = "member-uuid-001";
    private final String ACCESS_TOKEN = "access-token-abc";

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new CustomUserDetails(MEMBER_ID, ACCESS_TOKEN);
    }

    // =========================================================================
    // registerMember
    // =========================================================================
    @Test
    void registerMember_shouldDelegateToServiceAndReturnResponse() {
        RegisterRequest request = new RegisterRequest();
        BaseResponse<RegisterResponse> expected = new BaseResponse<>("VYRA-REG-000", "Success", new RegisterResponse());
        when(authenticationService.registerMember(request)).thenReturn(expected);

        BaseResponse<RegisterResponse> result = authenticationController.registerMember(request);

        assertThat(result).isSameAs(expected);
        verify(authenticationService).registerMember(request);
    }

    // =========================================================================
    // resendOtp
    // =========================================================================
    @Test
    void resendOtp_shouldDelegateToServiceAndReturnResponse() {
        ResendOtpRequest request = new ResendOtpRequest();
        BaseResponse<Void> expected = new BaseResponse<>("VYRA-OTP-001", "Resent", null);
        when(authenticationService.resendOtp(request)).thenReturn(expected);

        BaseResponse<Void> result = authenticationController.resendOtp(request);

        assertThat(result).isSameAs(expected);
        verify(authenticationService).resendOtp(request);
    }

    // =========================================================================
    // verifyOtp
    // =========================================================================
    @Test
    void verifyOtp_shouldDelegateToServiceAndReturnResponse() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        BaseResponse<Void> expected = new BaseResponse<>("VYRA-OTP-000", "Verified", null);
        when(authenticationService.verifyOtp(request)).thenReturn(expected);

        BaseResponse<Void> result = authenticationController.verifyOtp(request);

        assertThat(result).isSameAs(expected);
        verify(authenticationService).verifyOtp(request);
    }

    // =========================================================================
    // loginMember
    // =========================================================================
    @Test
    void loginMember_shouldDelegateToServiceAndReturnResponse() {
        LoginRequest request = new LoginRequest();
        BaseResponse<LoginResponse> expected = new BaseResponse<>("VYRA-LGN-000", "Logged in", new LoginResponse());
        when(authenticationService.loginMember(request)).thenReturn(expected);

        BaseResponse<LoginResponse> result = authenticationController.loginMember(request);

        assertThat(result).isSameAs(expected);
        verify(authenticationService).loginMember(request);
    }

    // =========================================================================
    // refreshToken
    // =========================================================================
    @Test
    void refreshToken_shouldDelegateToServiceAndReturnResponse() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        BaseResponse<RefreshTokenResponse> expected = new BaseResponse<>("VYRA-TKN-005", "Refreshed", new RefreshTokenResponse());
        when(authenticationService.refreshToken(request)).thenReturn(expected);

        BaseResponse<RefreshTokenResponse> result = authenticationController.refreshToken(request);

        assertThat(result).isSameAs(expected);
        verify(authenticationService).refreshToken(request);
    }

    // =========================================================================
    // forgotPin
    // =========================================================================
    @Test
    void forgotPin_shouldDelegateToServiceAndReturnResponse() {
        ForgotPinRequest request = new ForgotPinRequest();
        BaseResponse<Void> expected = new BaseResponse<>("VYRA-FP-001", "OTP sent", null);
        when(authenticationService.forgotPin(request)).thenReturn(expected);

        BaseResponse<Void> result = authenticationController.forgotPin(request);

        assertThat(result).isSameAs(expected);
        verify(authenticationService).forgotPin(request);
    }

    // =========================================================================
    // resetPin
    // =========================================================================
    @Test
    void resetPin_shouldDelegateToServiceAndReturnResponse() {
        ResetPinRequest request = new ResetPinRequest();
        BaseResponse<Void> expected = new BaseResponse<>("VYRA-FP-002", "PIN reset", null);
        when(authenticationService.resetPin(request)).thenReturn(expected);

        BaseResponse<Void> result = authenticationController.resetPin(request);

        assertThat(result).isSameAs(expected);
        verify(authenticationService).resetPin(request);
    }

    // =========================================================================
    // logoutMember
    // =========================================================================
    @Test
    void logoutMember_shouldExtractPrincipalAndDelegateToService() {
        BaseResponse<Void> expected = new BaseResponse<>("VYRA-LGT-000", "Logged out", null);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationService.logoutMember(MEMBER_ID, ACCESS_TOKEN)).thenReturn(expected);

        BaseResponse<Void> result = authenticationController.logoutMember(authentication);

        assertThat(result).isSameAs(expected);
        verify(authentication).getPrincipal();
        verify(authenticationService).logoutMember(MEMBER_ID, ACCESS_TOKEN);
    }
}
