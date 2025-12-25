package com.vyra.virtual_your_assets.controller;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.register.ForgotPasswordRequest;
import com.vyra.virtual_your_assets.dto.register.ResetPasswordRequest;
import com.vyra.virtual_your_assets.service.MemberService;
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
class MemberControllerTest {
    @Mock
    private MemberService service;
    @InjectMocks
    private MemberController controller;

    @Test
    void forgotPasswordSuccess() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setPhoneNumber("62811111111");

        BaseResponse<Void> mockResponse = new BaseResponse<>(
                ErrorConstant.FORGOT_PASSWORD_OTP_SENT.getCode(),
                ErrorConstant.FORGOT_PASSWORD_OTP_SENT.getMessage(),
                null
        );

        when(service.forgotPassword(any(ForgotPasswordRequest.class))).thenReturn(mockResponse);

        ResponseEntity<BaseResponse<Void>> responseEntity = controller.forgotPassword(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
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

        when(service.resetPassword(any(ResetPasswordRequest.class))).thenReturn(mockResponse);

        ResponseEntity<BaseResponse<Void>> responseEntity = controller.resetPassword(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }
}