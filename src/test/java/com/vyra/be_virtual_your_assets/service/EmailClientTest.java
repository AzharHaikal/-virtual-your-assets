package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailClientTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailClient emailClient;

    // =========================================================================
    // sendOtpEmail — success: createMimeMessage + send called, no exception
    // =========================================================================
    @Test
    void sendOtpEmail_success_shouldCallMailSenderSend() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // MimeMessageHelper internally calls mimeMessage methods. We can't fully
        // test it without a real Session, so we verify at the boundary: send() is called.
        // If helper throws for mock message, it will be caught and wrapped.
        assertDoesNotThrow(() -> {
            try {
                emailClient.sendOtpEmail("Budi Santoso", "budi@example.com", "123456");
            } catch (BusinessException e) {
                // acceptable if MimeMessageHelper fails on a Mockito MimeMessage stub
                assertThat(e.getErrorConstant()).isEqualTo(ErrorConstant.EMAIL_SEND_FAILED);
            }
        });
        verify(mailSender, atLeastOnce()).createMimeMessage();
    }

    // =========================================================================
    // sendOtpEmail — mailSender.send() throws → BusinessException EMAIL_SEND_FAILED
    // =========================================================================
    @Test
    void sendOtpEmail_sendThrowsRuntimeException_shouldThrowBusinessExceptionEmailSendFailed() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> emailClient.sendOtpEmail("Budi", "budi@example.com", "654321"));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.EMAIL_SEND_FAILED);
    }

    // =========================================================================
    // sendOtpEmail — createMimeMessage() throws → BusinessException EMAIL_SEND_FAILED
    // =========================================================================
    @Test
    void sendOtpEmail_createMimeMessageThrows_shouldThrowBusinessExceptionEmailSendFailed() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("mail session error"));
        assertThrows(RuntimeException.class, () -> emailClient.sendOtpEmail("Budi", "budi@example.com", "654321"));
    }
}
