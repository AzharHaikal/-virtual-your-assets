package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
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
    // private final WhatsAppClient whatsAppClient;

    @Async
    public void sendOtp(String fullName, String email, String phoneNumber, String otp) {
        // TODO: Send OTP via WhatsApp
        try {
            emailClient.sendOtpEmail(fullName, email, otp);
            /* whatsAppClient.sendMessage(phoneNumber, message); */
        } catch (Exception e) {
            log.error("[ERROR] resendOtp {} encountered an exception: {}", phoneNumber, e.getMessage(), e);
            throw new BusinessException(ErrorConstant.EMAIL_SEND_FAILED);
        }
    }

    @Async
    public void sendPrankHack(String fullName, String email) {
        try {
            emailClient.sendPrankHack(fullName, email);
        } catch (Exception e) {
            throw new BusinessException(ErrorConstant.INTERNAL_SERVER_ERROR);
        }
    }

}
