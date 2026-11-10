package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.Map;

/**
 * OTP and data-export e-mails, always in English (mail apps such as Gmail offer a built-in translation).
 * Delivered over SMTP ({@code MAIL_PROVIDER=smtp}, the default, e.g. Gmail with an app password) or over
 * the Brevo HTTP API ({@code MAIL_PROVIDER=brevo}, port 443) for hosts that block outgoing SMTP such as
 * Render Free. The Brevo sender address has to be verified in Brevo.
 *
 * <p>Logo: {@code MAIL_LOGO_URL} (a public https image, e.g. the portal's {@code /logo-icon.png}) when set;
 * otherwise SMTP mails carry {@code mail/vyra-logo.png} as an inline image and Brevo mails show the
 * VYRA wordmark only.
 */
@Service
@Slf4j
public class EmailClient {

    static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
    static final String LOGO_CID = "vyra-logo";
    private static final String LOGO_RESOURCE = "mail/vyra-logo.png";

    /** VYRA palette (landing, portal and app). */
    private static final String BLUE = "#0754E8";
    private static final String BLUE_LIGHT = "#1685FF";
    private static final String INK = "#0B1B3A";
    private static final String TEXT = "#334155";
    private static final String MUTED = "#64748B";
    private static final String LINE = "#E2E8F0";
    private static final String CANVAS = "#F1F5FB";
    private static final String FONT = "'Plus Jakarta Sans','Segoe UI',Roboto,Helvetica,Arial,sans-serif";

    private final JavaMailSender mailSender;
    private final RestClient restClient;
    private final boolean brevo;
    private final String brevoApiKey;
    private final String fromAddress;
    private final String fromName;
    private final String logoUrl;

    @Autowired
    public EmailClient(JavaMailSender mailSender,
                       RestClient.Builder restClientBuilder,
                       @Value("${vyra.mail.provider:smtp}") String provider,
                       @Value("${vyra.mail.brevo.api-key:}") String brevoApiKey,
                       @Value("${vyra.mail.from-address:}") String fromAddress,
                       @Value("${vyra.mail.from-name:VYRA}") String fromName,
                       @Value("${vyra.mail.logo-url:}") String logoUrl) {
        this.mailSender = mailSender;
        this.restClient = restClientBuilder.build();
        this.brevo = "brevo".equalsIgnoreCase(provider == null ? "" : provider.trim());
        this.brevoApiKey = brevoApiKey;
        this.fromAddress = fromAddress;
        this.fromName = isBlank(fromName) ? "VYRA" : fromName;
        this.logoUrl = isBlank(logoUrl) ? null : logoUrl.trim();
        if (brevo && (isBlank(brevoApiKey) || isBlank(fromAddress))) {
            log.warn("MAIL_PROVIDER=brevo needs BREVO_API_KEY and MAIL_FROM; e-mails will fail until both are set.");
        }
    }

    /** One-time code for registration, forgot PIN, contact change and account deletion. */
    public void sendOtpEmail(String fullName, String email, String otp) {
        String code = HtmlUtils.htmlEscape(otp == null ? "" : otp);
        String body =
                paragraph("Hi " + name(fullName) + ",") +
                paragraph("We received a request that needs your confirmation in the VYRA app. Enter this verification code to continue:") +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:28px 0 8px;\"><tr><td align=\"center\">" +
                "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>" +
                "<td style=\"background:#F0F6FF;border:1px solid #C9DBFD;border-radius:14px;padding:18px 34px;font-family:'SFMono-Regular',Consolas,'Liberation Mono',Menlo,monospace;font-size:34px;line-height:40px;font-weight:700;letter-spacing:10px;color:" + BLUE + ";\">" + code + "</td>" +
                "</tr></table></td></tr></table>" +
                "<p style=\"margin:0 0 28px;text-align:center;font-family:" + FONT + ";font-size:13px;line-height:20px;color:" + MUTED + ";\">This code expires in <strong style=\"color:" + INK + ";\">5 minutes</strong> and can be used once.</p>" +
                notice("Never share this code with anyone. The VYRA team will never ask for your code or your PIN by phone, chat or e-mail.") +
                paragraph("If you did not make this request, you can safely ignore this e-mail. Your account stays protected by your PIN.");
        deliver(email, fullName, "Your VYRA verification code: " + otp,
                layout("Your VYRA verification code is " + code + ". It expires in 5 minutes.", "Verify it’s you", body));
    }

    /** The personal data export is ready: a download link valid for 24 hours. */
    public void sendDataExportEmail(String firstName, String email, String downloadLink) {
        String link = HtmlUtils.htmlEscape(downloadLink == null ? "" : downloadLink);
        String body =
                paragraph("Hi " + name(firstName) + ",") +
                paragraph("The copy of your personal data that you requested in the VYRA app is ready. It contains your profile, finance records, mood check-ins with journal notes, habits and goals.") +
                button("Download my data", link) +
                "<p style=\"margin:0 0 24px;text-align:center;font-family:" + FONT + ";font-size:13px;line-height:20px;color:" + MUTED + ";\">The link works for <strong style=\"color:" + INK + ";\">24 hours</strong>. After that, request a new export from Profile &rarr; Download my data.</p>" +
                notice("The file contains your private journal. Do not forward this e-mail or share the link. If you did not request it, change your PIN in the app right away.") +
                "<p style=\"margin:20px 0 0;font-family:" + FONT + ";font-size:12px;line-height:18px;color:" + MUTED + ";\">Button not working? Copy this link into your browser:<br>" +
                "<a href=\"" + link + "\" style=\"color:" + BLUE + ";word-break:break-all;\">" + link + "</a></p>";
        deliver(email, firstName, "Your VYRA data export is ready",
                layout("Your VYRA data export is ready to download. The link works for 24 hours.", "Your data is ready", body));
    }

    // ------------------------------------------------------------------------------------------ delivery

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
            if (logoUrl == null) {
                helper.addInline(LOGO_CID, new ClassPathResource(LOGO_RESOURCE), "image/png");
            }
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

    // ------------------------------------------------------------------------------------------ template

    /** Logo source: the configured URL, the inline image (SMTP) or none (Brevo without a URL). */
    private String logoSrc() {
        if (logoUrl != null) {
            return HtmlUtils.htmlEscape(logoUrl);
        }
        return brevo ? null : "cid:" + LOGO_CID;
    }

    String layout(String preheader, String title, String body) {
        String logo = logoSrc();
        String brand = (logo == null ? "" :
                "<img src=\"" + logo + "\" width=\"52\" height=\"34\" alt=\"VYRA\" style=\"display:inline-block;vertical-align:middle;border:0;outline:none;width:52px;height:34px;margin-right:10px;\">") +
                "<span style=\"display:inline-block;vertical-align:middle;font-family:" + FONT + ";font-size:24px;line-height:34px;font-weight:800;letter-spacing:3px;color:" + INK + ";\">VYRA</span>";
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                "<meta name=\"color-scheme\" content=\"light\"><meta name=\"supported-color-schemes\" content=\"light\"><title>" + title + "</title></head>" +
                "<body style=\"margin:0;padding:0;background:" + CANVAS + ";-webkit-text-size-adjust:100%;\">" +
                "<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;\">" + preheader + "</div>" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background:" + CANVAS + ";\"><tr><td align=\"center\" style=\"padding:32px 16px;\">" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"max-width:560px;\">" +
                // brand row
                "<tr><td style=\"padding:0 4px 18px;\">" + brand + "</td></tr>" +
                // card
                "<tr><td style=\"background:#FFFFFF;border:1px solid " + LINE + ";border-radius:18px;overflow:hidden;\">" +
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">" +
                "<tr><td style=\"height:5px;line-height:5px;font-size:0;border-radius:18px 18px 0 0;background:" + BLUE + ";background-image:linear-gradient(90deg," + BLUE + "," + BLUE_LIGHT + ",#22D3EE);\">&nbsp;</td></tr>" +
                "<tr><td style=\"padding:36px 40px 34px;\">" +
                "<h1 style=\"margin:0 0 20px;font-family:" + FONT + ";font-size:24px;line-height:32px;font-weight:800;color:" + INK + ";\">" + title + "</h1>" +
                body +
                "</td></tr></table></td></tr>" +
                // footer
                "<tr><td style=\"padding:22px 12px 0;text-align:center;font-family:" + FONT + ";font-size:12px;line-height:19px;color:" + MUTED + ";\">" +
                "This is an automated message from VYRA, please do not reply.<br>" +
                "Need help? Open the VYRA app and go to Profile &rarr; Help.<br><br>" +
                "<span style=\"color:#94A3B8;\">&copy; 2026 VYRA &middot; Virtual Your Assets &middot; Your personal finance and wellbeing companion</span>" +
                "</td></tr>" +
                "</table></td></tr></table></body></html>";
    }

    private static String paragraph(String html) {
        return "<p style=\"margin:0 0 16px;font-family:" + FONT + ";font-size:15px;line-height:24px;color:" + TEXT + ";\">" + html + "</p>";
    }

    private static String notice(String text) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:0 0 20px;\"><tr>" +
                "<td style=\"background:#F8FAFC;border:1px solid " + LINE + ";border-left:4px solid " + BLUE + ";border-radius:10px;padding:14px 16px;font-family:" + FONT + ";font-size:13px;line-height:20px;color:" + TEXT + ";\">" +
                "<strong style=\"color:" + INK + ";\">Security tip:</strong> " + text + "</td></tr></table>";
    }

    private static String button(String label, String href) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:28px 0 10px;\"><tr><td align=\"center\">" +
                "<a href=\"" + href + "\" style=\"display:inline-block;background:" + BLUE + ";background-image:linear-gradient(135deg," + BLUE + "," + BLUE_LIGHT + ");color:#FFFFFF;text-decoration:none;font-family:" + FONT + ";font-size:15px;line-height:20px;font-weight:700;padding:14px 32px;border-radius:12px;\">" + label + "</a>" +
                "</td></tr></table>";
    }

    private static String name(String raw) {
        String n = raw == null ? "" : raw.trim();
        return n.isEmpty() ? "there" : "<strong style=\"color:" + INK + ";\">" + HtmlUtils.htmlEscape(n) + "</strong>";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
