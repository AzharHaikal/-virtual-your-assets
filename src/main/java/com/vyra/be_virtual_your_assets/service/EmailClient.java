package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;

/**
 * OTP and data-export e-mails. Delivered over SMTP ({@code MAIL_PROVIDER=smtp}, the default, e.g. Gmail
 * with an app password) or over the Brevo HTTP API ({@code MAIL_PROVIDER=brevo}, port 443) for hosts that
 * block outgoing SMTP such as Render Free. The Brevo sender address has to be verified in Brevo.
 */
@Service
@Slf4j
public class EmailClient {

    static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    /** VYRA blue palette (landing, portal and app). */
    private static final String BLUE = "#0754e8";
    private static final String BLUE_LIGHT = "#1685ff";
    private static final String INK = "#0b1b3a";

    private final JavaMailSender mailSender;
    private final RestClient restClient;
    private final boolean brevo;
    private final String brevoApiKey;
    private final String fromAddress;
    private final String fromName;

    @Autowired
    public EmailClient(JavaMailSender mailSender,
                       RestClient.Builder restClientBuilder,
                       @Value("${vyra.mail.provider:smtp}") String provider,
                       @Value("${vyra.mail.brevo.api-key:}") String brevoApiKey,
                       @Value("${vyra.mail.from-address:}") String fromAddress,
                       @Value("${vyra.mail.from-name:VYRA}") String fromName) {
        this.mailSender = mailSender;
        this.restClient = restClientBuilder.build();
        this.brevo = "brevo".equalsIgnoreCase(provider == null ? "" : provider.trim());
        this.brevoApiKey = brevoApiKey;
        this.fromAddress = fromAddress;
        this.fromName = fromName == null || fromName.isBlank() ? "VYRA" : fromName;
        if (brevo && (isBlank(brevoApiKey) || isBlank(fromAddress))) {
            log.warn("MAIL_PROVIDER=brevo needs BREVO_API_KEY and MAIL_FROM; e-mails will fail until both are set.");
        }
    }

    public void sendOtpEmail(String fullName, String email, String otp) {
        deliver(email, fullName, "VYRA - Kode verifikasi: " + otp, getOtpHtmlTemplate(fullName, otp));
    }

    /** The personal data export is ready: a download link valid for 24 hours. */
    public void sendDataExportEmail(String firstName, String email, String downloadLink, AppLanguage language) {
        boolean id = language == AppLanguage.ID;
        deliver(email, firstName, id ? "VYRA - Data Anda siap diunduh" : "VYRA - Your data is ready to download",
                getDataExportHtmlTemplate(firstName, downloadLink, id));
    }

    private void deliver(String to, String name, String subject, String html) {
        if (brevo) {
            sendWithBrevo(to, name, subject, html);
        } else {
            sendWithSmtp(to, subject, html);
        }
    }

    private void sendWithSmtp(String to, String subject, String html) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (!isBlank(fromAddress)) {
                helper.setFrom(fromAddress, fromName);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new BusinessException(ErrorConstant.EMAIL_SEND_FAILED);
        }
    }

    private void sendWithBrevo(String to, String name, String subject, String html) {
        Map<String, Object> recipient = isBlank(name) ? Map.of("email", to) : Map.of("email", to, "name", name.trim());
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", fromAddress == null ? "" : fromAddress, "name", fromName),
                "to", List.of(recipient),
                "subject", subject,
                "htmlContent", html);
        try {
            restClient.post()
                    .uri(BREVO_URL)
                    .header("api-key", brevoApiKey == null ? "" : brevoApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Brevo rejected the e-mail: {}", e.getMessage());
            throw new BusinessException(ErrorConstant.EMAIL_SEND_FAILED);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String header() {
        return "    <tr><td align=\"center\" style=\"background-color: " + BLUE + "; background-image: linear-gradient(135deg, " + BLUE + ", " + BLUE_LIGHT + "); padding: 40px 0;\">" +
                "      <h1 style=\"color: #ffffff; margin: 0; font-size: 32px; letter-spacing: 4px; font-weight: 800;\">VYRA</h1>" +
                "      <p style=\"color: #dbeafe; margin: 12px 0 0 0; font-size: 13px; letter-spacing: 3px;\">Virtual Your Assets</p>" +
                "    </td></tr>";
    }

    private static String footer() {
        return "    <tr><td style=\"padding: 22px 40px; background-color: #f5f8ff; border-top: 1px solid #e3ebfb; text-align: center;\">" +
                "      <p style=\"color: #64748b; font-size: 12px; margin: 0;\">&copy; 2026 VYRA. Catatan keuangan &amp; wellbeing pribadi.</p>" +
                "    </td></tr>";
    }

    private static String open() {
        return "<div style=\"font-family: 'Plus Jakarta Sans', 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #eef3fd; padding: 50px 0; margin: 0;\">" +
                "  <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"500\" style=\"background-color: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 30px rgba(7,84,232,0.12);\">";
    }

    private static String close() {
        return "  </table></div>";
    }

    private String getDataExportHtmlTemplate(String firstName, String downloadLink, boolean id) {
        String name = HtmlUtils.htmlEscape(firstName == null ? "" : firstName.trim());
        String link = HtmlUtils.htmlEscape(downloadLink);
        return open() + header() +
                "    <tr><td style=\"padding: 40px;\">" +
                "      <p style=\"color: " + INK + "; font-size: 16px;\">" + (id ? "Halo, " : "Hi, ") + "<strong>" + name + "</strong></p>" +
                "      <p style=\"color: #475569; font-size: 15px; line-height: 1.6;\">" +
                (id ? "Salinan seluruh data pribadi Anda sudah siap. Tautan di bawah ini berlaku selama 24 jam."
                    : "A copy of all your personal data is ready. The link below works for 24 hours.") + "</p>" +
                "      <div style=\"text-align: center; margin: 30px 0;\"><a href=\"" + link + "\" style=\"display: inline-block; padding: 14px 28px; background-color: " + BLUE + "; color: #ffffff; border-radius: 12px; text-decoration: none; font-weight: 700;\">" +
                (id ? "Unduh data" : "Download data") + "</a></div>" +
                "      <p style=\"color: #92400E; font-size: 13px; background-color: #FFFBEB; border-left: 4px solid #F59E0B; padding: 12px;\">" +
                (id ? "File ini berisi jurnal Anda. Jangan bagikan tautan ini. Bila Anda tidak memintanya, segera ganti PIN Anda."
                    : "The file contains your journal. Do not share this link. If you did not ask for it, change your PIN right away.") +
                "</p>" +
                "    </td></tr>" + footer() + close();
    }

    private String getOtpHtmlTemplate(String fullName, String otp) {
        String name = HtmlUtils.htmlEscape(fullName == null ? "" : fullName.trim());
        String code = HtmlUtils.htmlEscape(otp);
        return open() + header() +
                "    <tr><td style=\"padding: 40px;\">" +
                "      <h2 style=\"color: " + INK + "; font-size: 22px; margin-top: 0; font-weight: 700;\">Verifikasi Keamanan</h2>" +
                "      <p style=\"color: " + INK + "; font-size: 16px; line-height: 1.6;\">Halo, <strong>" + name + "</strong></p>" +
                "      <p style=\"color: #475569; font-size: 15px; line-height: 1.6;\">Kami menerima permintaan akses untuk akun VYRA Anda. Gunakan kode verifikasi di bawah ini untuk melanjutkan:</p>" +
                "      <div style=\"text-align: center; margin: 35px 0;\">" +
                "        <div style=\"display: inline-block; padding: 20px 40px; background-color: #f0f6ff; border: 1.5px solid #c7dafc; border-radius: 14px;\">" +
                "          <span style=\"font-size: 36px; font-weight: 800; color: " + BLUE + "; letter-spacing: 10px; font-family: monospace;\">" + code + "</span>" +
                "        </div>" +
                "        <p style=\"color: " + BLUE_LIGHT + "; font-size: 13px; margin-top: 15px; font-weight: 600;\">Berlaku selama 5 menit</p>" +
                "      </div>" +
                "      <div style=\"background-color: #FFFBEB; border-left: 4px solid #F59E0B; padding: 15px; margin-bottom: 20px;\">" +
                "        <p style=\"color: #92400E; font-size: 13px; margin: 0; line-height: 1.4;\"><strong>Penting:</strong> Jangan pernah membagikan kode ini kepada siapa pun. Tim VYRA tidak akan pernah meminta kode Anda melalui telepon atau pesan.</p>" +
                "      </div>" +
                "    </td></tr>" + footer() + close();
    }
}
