package vn.hcmute.rebook.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.hcmute.rebook.service.MailService;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${app.otp.expire-minutes}")
    private int expireMinutes;

    @Async
    @Override
    public void sendOtp(String to, String code, String purposeLabel) {
        if (from == null || from.isBlank()) {
            // Chưa cấu hình Gmail: in OTP ra console để demo
            log.warn("[OTP] Chưa cấu hình mail. Mã OTP {} cho {}: {}", purposeLabel, to, code);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, "ReBook - Chuỗi sách cũ");
            helper.setTo(to);
            helper.setSubject("[ReBook] Mã OTP " + purposeLabel);
            helper.setText("""
                    <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;border:1px solid #eee;border-radius:12px;padding:24px">
                      <h2 style="color:#8c4a2f;margin-top:0">ReBook</h2>
                      <p>Mã OTP %s của bạn là:</p>
                      <p style="font-size:32px;font-weight:bold;letter-spacing:8px;color:#8c4a2f">%s</p>
                      <p>Mã có hiệu lực trong %d phút. Không chia sẻ mã này cho bất kỳ ai.</p>
                    </div>
                    """.formatted(purposeLabel, code, expireMinutes), true);
            mailSender.send(message);
            log.info("Đã gửi OTP {} tới {}", purposeLabel, to);
        } catch (Exception e) {
            log.error("Gửi mail thất bại tới {}: {}. Mã OTP: {}", to, e.getMessage(), code);
        }
    }
}
