package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(MockitoExtension.class)
class EmailClientTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailClient smtp() {
        return new EmailClient(mailSender, RestClient.builder(), "smtp", "", "viraasst@gmail.com", "VYRA");
    }

    private static MimeMessage realMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    // ---- SMTP (default) ----

    @Test
    void smtp_otp_sendsBlueHtmlFromTheConfiguredSender() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(realMessage());

        smtp().sendOtpEmail("Budi <Santoso>", "budi@example.com", "123456");

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        MimeMessage message = sent.getValue();
        message.saveChanges();
        assertThat(message.getSubject()).isEqualTo("VYRA - Kode verifikasi: 123456");
        assertThat(message.getFrom()[0].toString()).contains("viraasst@gmail.com");
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        message.writeTo(out);
        String raw = out.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(raw).contains("#0754e8").doesNotContain("#0A1D37").doesNotContain("<Santoso>");
    }

    @Test
    void smtp_sendThrows_isWrappedAsEmailSendFailed() {
        when(mailSender.createMimeMessage()).thenReturn(realMessage());
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> smtp().sendOtpEmail("Budi", "budi@example.com", "654321"));
        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.EMAIL_SEND_FAILED);
    }

    @Test
    void smtp_dataExport_usesTheMemberLanguage() {
        when(mailSender.createMimeMessage()).thenReturn(realMessage());
        smtp().sendDataExportEmail("Budi", "budi@example.com", "https://x/y", AppLanguage.EN);

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue()).extracting(m -> {
            try { return m.getSubject(); } catch (Exception e) { return null; }
        }).isEqualTo("VYRA - Your data is ready to download");
    }

    // ---- Brevo HTTP API ----

    @Test
    void brevo_postsTheMessageWithTheApiKey_andNeverTouchesSmtp() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(EmailClient.BREVO_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "xkeysib-test"))
                .andExpect(jsonPath("$.sender.email").value("no-reply@vyra.test"))
                .andExpect(jsonPath("$.sender.name").value("VYRA"))
                .andExpect(jsonPath("$.to[0].email").value("budi@example.com"))
                .andExpect(jsonPath("$.subject").value("VYRA - Kode verifikasi: 111222"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).body("{\"messageId\":\"<1@brevo>\"}"));

        new EmailClient(mailSender, builder, "brevo", "xkeysib-test", "no-reply@vyra.test", "VYRA")
                .sendOtpEmail("Budi", "budi@example.com", "111222");

        server.verify();
        verifyNoInteractions(mailSender);
    }

    @Test
    void brevo_errorResponse_isWrappedAsEmailSendFailed() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(EmailClient.BREVO_URL)).andRespond(withBadRequest());

        EmailClient client = new EmailClient(mailSender, builder, "BREVO", "bad", "no-reply@vyra.test", "VYRA");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.sendDataExportEmail("Budi", "budi@example.com", "https://x/y", AppLanguage.ID));
        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.EMAIL_SEND_FAILED);
    }
}
