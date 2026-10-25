package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.SystemConfigKey;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private EmailClient emailClient;

    @Mock
    private SystemSettings systemSettings;

    @InjectMocks
    private OtpService otpService;

    private final String FULL_NAME = "Budi Santoso";
    private final String EMAIL = "budi@example.com";
    private final String PHONE = "628123456789";
    private final String OTP = "123456";

    @BeforeEach
    void emailOn() {
        lenient().when(systemSettings.isEnabled(SystemConfigKey.OTP_EMAIL_ENABLED)).thenReturn(true);
        lenient().when(systemSettings.isEnabled(SystemConfigKey.OTP_WHATSAPP_ENABLED)).thenReturn(false);
    }

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

    @Test
    void sendOtp_emailSwitchedOff_sendsNoEmail() {
        when(systemSettings.isEnabled(SystemConfigKey.OTP_EMAIL_ENABLED)).thenReturn(false);

        otpService.sendOtp(FULL_NAME, EMAIL, PHONE, OTP);

        verifyNoInteractions(emailClient);
    }
}
