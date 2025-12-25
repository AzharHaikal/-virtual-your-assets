package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailClientTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailClient emailClient;

    @Test
    void sendOtpEmailSuccess() {
        String fullName = "Azhar Haikal";
        String email = "test@example.com";
        String otp = "123456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailClient.sendOtpEmail(fullName, email, otp);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(mailSender, times(1)).createMimeMessage();
    }

    @Test
    void sendOtpEmailError() {
        String fullName = "Azhar Haikal";
        String email = "test@example.com";
        String otp = "123456";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        doThrow(new BusinessException(ErrorConstant.INTERNAL_SERVER_ERROR)).when(mailSender).send(any(MimeMessage.class));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                emailClient.sendOtpEmail(fullName, email, otp)
        );

        assertEquals(ErrorConstant.INTERNAL_SERVER_ERROR, ex.getErrorConstant());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}