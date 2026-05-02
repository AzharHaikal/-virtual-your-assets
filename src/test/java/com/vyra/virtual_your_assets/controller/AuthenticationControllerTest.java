package com.vyra.virtual_your_assets.controller;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.OtpType;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.login.LoginRequest;
import com.vyra.virtual_your_assets.dto.login.LoginResponse;
import com.vyra.virtual_your_assets.dto.register.*;
import com.vyra.virtual_your_assets.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {
    @Mock private AuthenticationService service;
    @InjectMocks private AuthenticationController controller;

    @Test
    void registerSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Azhar");
        request.setLastName("Haikal");
        request.setEmail("test@mail.com");
        request.setPhoneNumber("62811111111");
        request.setPin("123456");

        RegisterResponse registerResponse = new RegisterResponse();
        registerResponse.setMemberId("memberId");

        BaseResponse<RegisterResponse> mockResponse = new BaseResponse<>(
                ErrorConstant.REGISTER_SUCCESS.getCode(),
                ErrorConstant.REGISTER_SUCCESS.getMessage(),
                registerResponse
        );

        when(service.registerMember(any(RegisterRequest.class))).thenReturn(mockResponse);

        BaseResponse<RegisterResponse> response = controller.registerMember(request);
        assertEquals(ErrorConstant.REGISTER_SUCCESS.getCode(), response.getResponseStatus());
        assertEquals(ErrorConstant.REGISTER_SUCCESS.getMessage(), response.getResponseMessage());
    }

    @Test
    void resendOtpSuccess() {
        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail("azhar@mail.com");
        request.setOtpType(OtpType.REGISTER);

        BaseResponse<Void> mockResponse = new BaseResponse<>(
                ErrorConstant.RESEND_OTP.getCode(),
                ErrorConstant.RESEND_OTP.getMessage(),
                null
        );

        when(service.resendOtp(any(ResendOtpRequest.class))).thenReturn(mockResponse);

        BaseResponse<Void> response = controller.resendOtp(request);
        assertNotNull(response);
    }

    @Test
    void verifyOtpSuccess() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("62811111111");
        request.setOtpCode("123456");

        BaseResponse<Void> mockResponse = new BaseResponse<>(
                ErrorConstant.VERIFY_OTP_SUCCESS.getCode(),
                ErrorConstant.VERIFY_OTP_SUCCESS.getMessage(),
                null
        );

        when(service.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(mockResponse);

        BaseResponse<Void> response = controller.verifyOtp(request);
        assertNotNull(response);
    }

    @Test
    void loginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("628123456789");
        request.setPin("123456");

        LoginResponse loginData = new LoginResponse();
        loginData.setToken("mock-jwt-token");

        BaseResponse<LoginResponse> mockResponse = new BaseResponse<>(
                ErrorConstant.LOGIN_SUCCESS.getCode(),
                ErrorConstant.LOGIN_SUCCESS.getMessage(),
                loginData
        );

        when(service.loginMember(any(LoginRequest.class))).thenReturn(mockResponse);

        BaseResponse<LoginResponse> response = controller.loginMember(request);
        assertEquals(ErrorConstant.LOGIN_SUCCESS.getCode(), response.getResponseStatus());
        assertEquals(ErrorConstant.LOGIN_SUCCESS.getMessage(), response.getResponseMessage());
    }

    @Test
    void forgotPasswordSuccess() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("62811111111");

        BaseResponse<Void> mockResponse = new BaseResponse<>(
                ErrorConstant.FORGOT_PASSWORD_OTP_SENT.getCode(),
                ErrorConstant.FORGOT_PASSWORD_OTP_SENT.getMessage(),
                null
        );

        when(service.forgotPin(any(ForgotPasswordRequest.class))).thenReturn(mockResponse);

        BaseResponse<Void> response = controller.forgotPin(request);
        assertNotNull(response);
    }

    @Test
    void resetPasswordSuccess() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPhoneNumber("62811111111");
        request.setNewPin("654321");

        BaseResponse<Void> mockResponse = new BaseResponse<>(
                ErrorConstant.RESET_PIN_SUCCESS.getCode(),
                ErrorConstant.RESET_PIN_SUCCESS.getMessage(),
                null
        );

        when(service.resetPin(any(ResetPasswordRequest.class))).thenReturn(mockResponse);

        BaseResponse<Void> response = controller.resetPin(request);
        assertNotNull(response);
    }
}