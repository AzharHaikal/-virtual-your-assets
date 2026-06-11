package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private EmailClient emailClient;

    @InjectMocks
    private OtpService otpService;

    private final String FULL_NAME = "Budi Santoso";
    private final String EMAIL = "budi@example.com";
    private final String PHONE = "628123456789";
    private final String OTP = "123456";

    // =========================================================================
    // sendOtp — success
    // =========================================================================
    @Test
    void sendOtp_shouldCallEmailClientSendOtpEmail() {
        doNothing().when(emailClient).sendOtpEmail(FULL_NAME, EMAIL, OTP);

        otpService.sendOtp(FULL_NAME, EMAIL, PHONE, OTP);

        verify(emailClient).sendOtpEmail(FULL_NAME, EMAIL, OTP);
    }

    // =========================================================================
    // sendOtp — email client throws → BusinessException EMAIL_SEND_FAILED
    // =========================================================================
    @Test
    void sendOtp_whenEmailClientThrows_shouldThrowBusinessExceptionEmailSendFailed() {
        doThrow(new RuntimeException("SMTP error"))
                .when(emailClient).sendOtpEmail(FULL_NAME, EMAIL, OTP);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> otpService.sendOtp(FULL_NAME, EMAIL, PHONE, OTP));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.EMAIL_SEND_FAILED);
    }

}
