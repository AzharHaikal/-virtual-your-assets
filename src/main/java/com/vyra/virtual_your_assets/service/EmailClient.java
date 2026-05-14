package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.exception.BusinessException;
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
            throw new BusinessException(ErrorConstant.EMAIL_SEND_FAILED);
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

    ////////////////////////////////////////////////////////////////////
    public void sendPrankHack(String fullName, String email) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("[CLAY] Who Am I: No System Is Safe " + fullName);

            String htmlContent = getHtmlTemplatePrankHacker(fullName);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new BusinessException(ErrorConstant.EMAIL_SEND_FAILED);
        }
    }

    private String getHtmlTemplatePrankHacker(String fullName) {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SISTEM KRITIS: Akses Tidak Sah Terdeteksi</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #050505; font-family: 'Courier New', Courier, monospace; color: #00FF41;">
                <table border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #050505; min-height: 100vh;">
                    <tr>
                        <td align="center" style="padding: 20px;">
                            <table border="0" cellpadding="0" cellspacing="0" width="600" style="border: 2px solid #00FF41; background-color: #000000; border-radius: 5px; overflow: hidden; box-shadow: 0 0 20px rgba(0, 255, 65, 0.4);">
                                <tr>
                                    <td style="background-color: #1a1a1a; padding: 12px 20px; border-bottom: 2px solid #00FF41;">
                                        <table border="0" cellpadding="0" cellspacing="0" width="100%">
                                            <tr>
                                                <td style="color: #00FF41; font-weight: bold; font-size: 13px; letter-spacing: 1px;">
                                                    [SSH-SESSION: CLAY-GHOST-INTERNAL]
                                                </td>
                                                <td align="right">
                                                    <span style="color: #ff3333; font-size: 12px;">CONNECTED ●</span>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 30px; line-height: 1.5;">
                                        <div style="text-align: center; margin-bottom: 30px;">
                                            <pre style="font-size: 10px; line-height: 1; margin: 0; color: #ff3333;">
 ██████╗██╗      █████╗ ██╗   ██╗
██╔════╝██║     ██╔══██╗╚██╗ ██╔╝
██║     ██║     ███████║ ╚████╔╝ 
██║     ██║     ██╔══██║  ╚██╔╝  
╚██████╗███████╗██║  ██║   ██║   
 ╚═════╝╚══════╝╚═╝  ╚═╝   ╚═╝   
                                            </pre>
                                            <div style="margin-top: 10px; font-weight: bold; font-size: 18px; color: #ff3333; background: rgba(255,51,51,0.1); padding: 5px;">
                                                *** SYSTEM BREACH DETECTED ***
                                            </div>
                                        </div>
                                        <div style="font-size: 13px; border-bottom: 1px solid #333; padding-bottom: 15px; margin-bottom: 20px;">
                                            <span style="color: #888 !important;">[SOURCE_IP]:</span> 192.168.1.104 <br>
                                            <span style="color: #888 !important;">[TARGET_IP]:</span> 10.0.8.254 (HIDDEN) <br>
                                            <span style="color: #888 !important;">[LOCATION]:</span> Unknown (Encrypted via Proxy) <br>
                                            <span style="color: #888 !important;">[ID]:</span> %s <br>
                                        </div>
                                        <div style="font-size: 14px; margin-bottom: 25px;">
                                            <div style="margin-bottom: 5px;">> bypass --firewall ... <span style="color: #ffffff;">[SUCCESS]</span></div>
                                            <div style="margin-bottom: 5px;">> brute-force --password ... <span style="color: #ffffff;">[FOUND]</span></div>
                                            <div style="margin-bottom: 5px;">> get --personal-files ... <span style="color: #ffffff;">[DOWNLOADED]</span></div>
                                            <div style="margin-bottom: 5px;">> inject --clay-payload ... <span style="color: #ffffff;">[ACTIVE]</span></div>
                                        </div>
                                        <div style="background-color: #111; border: 1px solid #ff3333; padding: 15px; text-align: center; margin-bottom: 30px;">
                                            <span style="color: #ff3333; font-weight: bold; font-size: 16px;">YOU ARE BEING MONITORED!.</span><br>
                                            <span style="font-size: 12px;">All data has been backed up to our central server.</span>
                                        </div>
                                        <div style="font-size: 11px; color: #006622; overflow: hidden; white-space: nowrap; margin-bottom: 20px;">
                                            01001000 01100001 01100011 01101011 01100101 01100100 00100000 01100010 01111001 00100000 01000011 01001100 01000001 01011001
                                            01101110 01101111 00100000 01110011 01111001 01110011 01110100 01100101 01101101 00100000 01101001 01110011 00100000 01110011
                                        </div>
                                        <table border="0" cellpadding="0" cellspacing="0" width="100%">
                                            <tr>
                                                <td align="center">
                                                    <div style="display: inline-block; background-color: #ff3333; color: #ffffff; padding: 12px 30px; font-weight: bold; border-radius: 3px; cursor: pointer; letter-spacing: 2px;">
                                                        PAY FOR SAVE ACCOUNT
                                                    </div>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 20px; background-color: #0a0a0a; text-align: center; border-top: 1px solid #222;">
                                        <div style="color: #444; font-size: 10px; margin-bottom: 5px;">
                                            CLAY CRYPTO-PROTOCOL-ID: 77-00-DE-AD-BE-EF
                                        </div>
                                        <div style="color: #222; font-size: 9px;">
                                            YOUR SYSTEM IS SHUTDOWN.
                                        </div>
                                    </td>
                                </tr>
                            </table>
                            <div style="height: 40px;"></div>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;

        // Menggunakan String.format untuk memasukkan nama ke dalam %s di template
//        return String.format(html, fullName.replace("%", "%%"));
//        return String.format(html, fullName);
        return html.replace("%s", fullName);
    }
}
