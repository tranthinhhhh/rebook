package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hcmute.rebook.entity.OtpToken;
import vn.hcmute.rebook.entity.enums.OtpPurpose;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.repository.OtpTokenRepository;
import vn.hcmute.rebook.service.MailService;
import vn.hcmute.rebook.service.OtpService;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private static final int MAX_ATTEMPTS = 5;
    private final SecureRandom random = new SecureRandom();

    private final OtpTokenRepository otpRepository;
    private final MailService mailService;

    @Value("${app.otp.expire-minutes}")
    private int expireMinutes;

    @Override
    @Transactional
    public void generateAndSend(String email, OtpPurpose purpose) {
        // Chặn gửi liên tục: phải cách lần trước ít nhất 60 giây
        otpRepository.findFirstByEmailAndPurposeAndUsedFalseOrderByIdDesc(email, purpose).ifPresent(old -> {
            if (old.getExpiresAt().minusMinutes(expireMinutes).plusSeconds(60).isAfter(LocalDateTime.now())) {
                throw new BusinessException("Vui lòng đợi 60 giây trước khi yêu cầu mã mới");
            }
            old.setUsed(true);
        });
        String code = String.format("%06d", random.nextInt(1_000_000));
        otpRepository.save(OtpToken.builder()
                .email(email).code(code).purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(expireMinutes))
                .build());
        mailService.sendOtp(email, code, purpose == OtpPurpose.REGISTER ? "kích hoạt tài khoản" : "đặt lại mật khẩu");
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void verify(String email, String code, OtpPurpose purpose) {
        OtpToken token = otpRepository.findFirstByEmailAndPurposeAndUsedFalseOrderByIdDesc(email, purpose)
                .orElseThrow(() -> new BusinessException("Mã OTP không tồn tại, vui lòng yêu cầu mã mới"));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Mã OTP đã hết hạn, vui lòng yêu cầu mã mới");
        }
        if (token.getAttempts() >= MAX_ATTEMPTS) {
            throw new BusinessException("Nhập sai quá nhiều lần, vui lòng yêu cầu mã mới");
        }
        if (!token.getCode().equals(code == null ? "" : code.trim())) {
            token.setAttempts(token.getAttempts() + 1);
            throw new BusinessException("Mã OTP không đúng (còn " + (MAX_ATTEMPTS - token.getAttempts()) + " lần thử)");
        }
        token.setUsed(true);
    }
}
