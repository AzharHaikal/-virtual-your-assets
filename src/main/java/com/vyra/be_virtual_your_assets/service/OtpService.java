package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.SystemConfigKey;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    private final EmailClient emailClient;
    private final SystemSettings systemSettings;
    // private final WhatsAppClient whatsAppClient;

    /**
     * Sends the code on the channels switched on in the portal (Settings > OTP). E-mail stays on until a
     * WhatsApp provider exists (SystemConfigService refuses to switch both off).
     */
    @Async
    public void sendOtp(String fullName, String email, String phoneNumber, String otp) {
        if (systemSettings.isEnabled(SystemConfigKey.OTP_WHATSAPP_ENABLED)) {
            // TODO: Send OTP via WhatsApp once WhatsAppClient has a provider.
            log.warn("WhatsApp OTP is switched on but no WhatsApp provider is configured. phoneNumber: {}", phoneNumber);
        }
        if (!systemSettings.isEnabled(SystemConfigKey.OTP_EMAIL_ENABLED)) {
            return;
        }
        try {
            emailClient.sendOtpEmail(fullName, email, otp);
            /* whatsAppClient.sendMessage(phoneNumber, message); */
        } catch (Exception e) {
            log.warn("resendOtp {} encountered an exception: {}", phoneNumber, e.getMessage());
            throw new BusinessException(ErrorConstant.EMAIL_SEND_FAILED);
        }
    }

    /*
    @Async
    public void sendPrankHack(String fullName, String email) {
        try {
            emailClient.sendPrankHack(fullName, email);
        } catch (Exception e) {
            throw new BusinessException(ErrorConstant.INTERNAL_SERVER_ERROR);
        }
    }
    */

}
