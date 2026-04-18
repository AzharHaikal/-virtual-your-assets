package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.exception.BusinessException;
import lombok.RequiredArgsConstructor;
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
            emailClient.sendOtpEmail(fullName, email, otp);
            /*
            Not used for now
            String message = String.format(
                    "VYRA Verification Code: %s\nThis code is confidential and valid for a limited time.",
                    otp
            );
            whatsAppClient.sendMessage(phoneNumber, message);
            */
        } catch (Exception ex) {
            throw new BusinessException(ErrorConstant.EMAIL_SEND_FAILED);
        }
    }

}
