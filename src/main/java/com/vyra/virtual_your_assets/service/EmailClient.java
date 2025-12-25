package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.dto.register.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailClient {
    private final JavaMailSender mailSender;

    public void sendOtpEmail(String fullName, String email, String otp) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("VYRA - Verification Code: " + otp);

            String htmlContent = getOtpHtmlTemplate(fullName, otp);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            // Handle exception (log error)
            e.printStackTrace();
        }
    }

    private String getOtpHtmlTemplate(String fullName, String otp) {
        return "<div style=\"font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f4f7f9; padding: 50px 0; margin: 0;\">" +
                "  <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"500\" style=\"background-color: #ffffff; border-radius: 20px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.1);\">" +
                "    <tr>" +
                "      <td align=\"center\" style=\"background-color: #0A1D37; padding: 40px 0;\">" +
                "        <h1 style=\"color: #ffffff; margin: 0; font-size: 32px; letter-spacing: 4px; font-weight: 800;\">VYRA</h1>" +
                "        <p style=\"color: #E6E6E6; margin: 15px 0 0 0; font-size: 14px; letter-spacing: 3px;\">Virtualize Your Assets</p>" +
                "      </td>" +
                "    </tr>" +
                "    <tr>" +
                "      <td style=\"padding: 40px 40px;\">" +
                "        <h2 style=\"color: #1A1A1A; font-size: 22px; margin-top: 0; font-weight: 700;\">Verifikasi Keamanan</h2>" +
                "        <p style=\"color: #444444; font-size: 16px; line-height: 1.6;\">Halo, <strong>" + fullName + "</strong></p>" +
                "        <p style=\"color: #666666; font-size: 15px; line-height: 1.6;\">Kami menerima permintaan akses untuk akun VYRA Anda. Gunakan kode verifikasi di bawah ini untuk melanjutkan:</p>" +
                "        " +
                "        <div style=\"text-align: center; margin: 35px 0;\">" +
                "          <div style=\"display: inline-block; padding: 20px 40px; background-color: #F8FAFC; border: 1.5px solid #E2E8F0; border-radius: 12px;\">" +
                "            <span style=\"font-size: 36px; font-weight: 800; color: #0A1D37; letter-spacing: 10px; font-family: monospace;\">" + otp + "</span>" +
                "          </div>" +
                "          <p style=\"color: #0056b3; font-size: 13px; margin-top: 15px; font-weight: 600;\">Berlaku selama 5 menit</p>" +
                "        </div>" +
                "        " +
                "        <div style=\"background-color: #FFFBEB; border-left: 4px solid #F59E0B; padding: 15px; margin-bottom: 20px;\">" +
                "          <p style=\"color: #92400E; font-size: 13px; margin: 0; line-height: 1.4;\"><strong>Penting:</strong> Jangan pernah membagikan kode ini kepada siapapun. Tim VYRA tidak akan pernah meminta kode Anda melalui telepon atau pesan singkat.</p>" +
                "        </div>" +
                "      </td>" +
                "    </tr>" +
                "    <tr>" +
                "      <td style=\"padding: 25px 40px; background-color: #F9FAFB; border-top: 1px solid #EDF2F7; text-align: center;\">" +
                "        <p style=\"color: #718096; font-size: 12px; margin: 0;\">&copy; 2025 VYRA Official. <br> Digital Asset Management System.</p>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</div>";
    }
}
