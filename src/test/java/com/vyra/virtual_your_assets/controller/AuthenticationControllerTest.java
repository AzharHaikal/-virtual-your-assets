package com.vyra.virtual_your_assets.controller;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.OtpType;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.login.LoginRequest;
import com.vyra.virtual_your_assets.dto.login.LoginResponse;
import com.vyra.virtual_your_assets.dto.register.RegisterRequest;
import com.vyra.virtual_your_assets.dto.register.RegisterResponse;
import com.vyra.virtual_your_assets.dto.register.ResendOtpRequest;
import com.vyra.virtual_your_assets.dto.register.VerifyOtpRequest;
import com.vyra.virtual_your_assets.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {
    @Mock
    private AuthenticationService service;
    @InjectMocks
    private AuthenticationController controller;

    @Test
    void registerSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Azhar");
        request.setLastName("Haikal");
        request.setEmail("test@mail.com");
        request.setPhoneNumber("62811111111");
        request.setPin("123456");

        RegisterResponse regResponse = new RegisterResponse();
        regResponse.setMemberId(UUID.randomUUID());

        BaseResponse<RegisterResponse> mockResponse = new BaseResponse<>(
                ErrorConstant.REGISTER_SUCCESS.getCode(),
                ErrorConstant.REGISTER_SUCCESS.getMessage(),
                regResponse
        );

        when(service.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        ResponseEntity<BaseResponse<RegisterResponse>> responseEntity = controller.register(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
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

        ResponseEntity<BaseResponse<Void>> responseEntity = controller.resendOtp(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
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

        ResponseEntity<BaseResponse<Void>> responseEntity = controller.verifyOtp(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
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

        when(service.login(any(LoginRequest.class))).thenReturn(mockResponse);

        ResponseEntity<BaseResponse<LoginResponse>> responseEntity = controller.login(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }
}