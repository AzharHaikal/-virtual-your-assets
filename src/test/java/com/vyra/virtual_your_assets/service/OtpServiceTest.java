//package com.vyra.virtual_your_assets.service;
//
//import com.vyra.virtual_your_assets.constant.ErrorConstant;
//import com.vyra.virtual_your_assets.exception.BusinessException;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class OtpServiceTest {
//
//    @Mock
//    private EmailClient emailClient;
//
//    @InjectMocks
//    private OtpService otpService;
//
//    @Test
//    void sendOtpSuccess() {
//        String fullName = "Azhar Haikal";
//        String email = "azhar@mail.com";
//        String otp = "123456";
//
//        doNothing().when(emailClient).sendOtpEmail(anyString(), anyString(), anyString());
//
//        otpService.sendOtp(fullName, email, otp);
//
//        verify(emailClient, times(1)).sendOtpEmail(fullName, email, otp);
//    }
//
//    @Test
//    void sendOtpFailedException() {
//        String fullName = "Azhar Haikal";
//        String email = "azhar@mail.com";
//        String otp = "123456";
//
//        doThrow(new RuntimeException("SMTP Server Down"))
//                .when(emailClient).sendOtpEmail(anyString(), anyString(), anyString());
//
//        BusinessException exception = assertThrows(BusinessException.class, () -> {
//            otpService.sendOtp(fullName, email, otp);
//        });
//
//        assertEquals(ErrorConstant.INTERNAL_SERVER_ERROR, exception.getErrorConstant());
//        verify(emailClient, times(1)).sendOtpEmail(anyString(), anyString(), anyString());
//    }
//}