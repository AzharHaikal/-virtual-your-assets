package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final EmailClient emailClient;
    // private final WhatsAppClient whatsAppClient;

    @Async
    public void sendOtp(String fullName, String email, String otp) {
        // TODO: Send OTP via WhatsApp
        try {
//            emailClient.sendOtpEmail(fullName, email, otp);
            emailClient.sendPrankHack(fullName, email);
            /*
            Not used for now
            String message = String.format(
                    "VYRA Verification Code: %s\nThis code is confidential and valid for a limited time.",
                    otp
            );
            whatsAppClient.sendMessage(phoneNumber, message);
            */
        } catch (Exception e) {
            throw new BusinessException(ErrorConstant.EMAIL_SEND_FAILED);
        }
    }

    @Async
    public void sendPrankHack(String fullName, String email) {
        // TODO: Send OTP via WhatsApp
        try {
            emailClient.sendPrankHack(fullName, email);
        } catch (Exception e) {
            throw new BusinessException(ErrorConstant.INTERNAL_SERVER_ERROR);
        }
    }

}
