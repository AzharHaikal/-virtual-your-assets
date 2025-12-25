package com.vyra.virtual_your_assets.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

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

        doThrow(new RuntimeException("SMTP Connection Error")).when(mailSender).send(any(MimeMessage.class));

        emailClient.sendOtpEmail(fullName, email, otp);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}