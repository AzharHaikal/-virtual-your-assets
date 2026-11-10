package com.vyra.be_virtual_your_assets.service;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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

    private EmailClient smtp(String logoUrl) {
        return new EmailClient(mailSender, RestClient.builder(), "smtp", "", "viraasst@gmail.com", "VYRA", logoUrl);
    }

    private static MimeMessage realMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private MimeMessage sent() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private static String raw(MimeMessage message) throws Exception {
        message.saveChanges();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    // ---- content: English, VYRA palette, escaped name ----

    @Test
    void otp_isEnglish_blue_andEscapesTheName() {
        String html = smtp("").layout("pre", "Verify it’s you", "body");
        assertThat(html).contains("<html lang=\"en\"").contains("#0754E8").contains("Profile &rarr; Help");

        when(mailSender.createMimeMessage()).thenReturn(realMessage());
        smtp("").sendOtpEmail("Budi <Santoso>", "budi@example.com", "123456");
        MimeMessage message = sent();
        assertThat(message).extracting(m -> { try { return m.getSubject(); } catch (Exception e) { return null; } })
                .isEqualTo("Your VYRA verification code: 123456");
    }

    @Test
    void smtp_withoutLogoUrl_attachesTheInlineLogo() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(realMessage());
        smtp("").sendOtpEmail("Budi", "budi@example.com", "123456");

        String raw = raw(sent());
        assertThat(raw).contains("Content-ID: <" + EmailClient.LOGO_CID + ">").contains("image/png");
        assertThat(raw).contains("viraasst@gmail.com");
        assertThat(raw).doesNotContain("Verifikasi").doesNotContain("Halo");
    }

    @Test
    void smtp_withLogoUrl_linksTheHostedLogo_andHasNoInlineImage() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(realMessage());
        smtp("https://portal.vyra.test/logo-icon.png").sendDataExportEmail("Budi", "budi@example.com", "https://x/y?token=a&b");

        MimeMessage message = sent();
        assertThat(message.getSubject()).isEqualTo("Your VYRA data export is ready");
        String raw = raw(message);
        assertThat(raw).doesNotContain("Content-ID:");
    }

    @Test
    void smtp_sendThrows_isWrappedAsEmailSendFailed() {
        when(mailSender.createMimeMessage()).thenReturn(realMessage());
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> smtp("").sendOtpEmail("Budi", "budi@example.com", "654321"));
        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.EMAIL_SEND_FAILED);
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
                .andExpect(jsonPath("$.subject").value("Your VYRA verification code: 111222"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://portal.vyra.test/logo-icon.png")))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{\"messageId\":\"<1@brevo>\"}"));

        new EmailClient(mailSender, builder, "brevo", "xkeysib-test", "no-reply@vyra.test", "VYRA", "https://portal.vyra.test/logo-icon.png")
                .sendOtpEmail("Budi", "budi@example.com", "111222");

        server.verify();
        verifyNoInteractions(mailSender);
    }

    @Test
    void brevo_withoutLogoUrl_hasNoBrokenImage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(EmailClient.BREVO_URL))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<img"))))
                .andRespond(withStatus(HttpStatus.CREATED));

        new EmailClient(mailSender, builder, "brevo", "k", "no-reply@vyra.test", "VYRA", "")
                .sendOtpEmail("Budi", "budi@example.com", "111222");
        server.verify();
    }

    @Test
    void brevo_errorResponse_isWrappedAsEmailSendFailed() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(EmailClient.BREVO_URL)).andRespond(withBadRequest());

        EmailClient client = new EmailClient(mailSender, builder, "BREVO", "bad", "no-reply@vyra.test", "VYRA", "");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.sendDataExportEmail("Budi", "budi@example.com", "https://x/y"));
        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.EMAIL_SEND_FAILED);
    }
}
