package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hcmute.rebook.dto.ChangePasswordForm;
import vn.hcmute.rebook.dto.RegisterForm;
import vn.hcmute.rebook.dto.ResetPasswordForm;
import vn.hcmute.rebook.entity.User;
import vn.hcmute.rebook.entity.enums.OtpPurpose;
import vn.hcmute.rebook.entity.enums.Role;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.repository.UserRepository;
import vn.hcmute.rebook.service.AuthService;
import vn.hcmute.rebook.service.OtpService;

@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = BusinessException.class)
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Override
    public void register(RegisterForm form) {
        String email = normalize(form.getEmail());
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null && existing.isEnabled()) {
            throw new BusinessException("Email đã được đăng ký");
        }
        User user = existing != null ? existing : new User();
        // Tài khoản chưa kích hoạt được phép đăng ký lại (cập nhật thông tin)
        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setPhone(form.getPhone().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.USER);
        user.setEnabled(false);
        userRepository.save(user);
        otpService.generateAndSend(email, OtpPurpose.REGISTER);
    }

    @Override
    public void activate(String email, String otp) {
        User user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại"));
        if (user.isEnabled()) throw new BusinessException("Tài khoản đã được kích hoạt, vui lòng đăng nhập");
        otpService.verify(user.getEmail(), otp, OtpPurpose.REGISTER);
        user.setEnabled(true);
    }

    @Override
    public void resendRegisterOtp(String email) {
        User user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại"));
        if (user.isEnabled()) throw new BusinessException("Tài khoản đã được kích hoạt");
        otpService.generateAndSend(user.getEmail(), OtpPurpose.REGISTER);
    }

    @Override
    public void sendResetOtp(String email) {
        User user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new BusinessException("Email chưa được đăng ký"));
        if (!user.isEnabled()) throw new BusinessException("Tài khoản chưa kích hoạt");
        otpService.generateAndSend(user.getEmail(), OtpPurpose.RESET_PASSWORD);
    }

    @Override
    public void resetPassword(ResetPasswordForm form) {
        User user = userRepository.findByEmail(normalize(form.getEmail()))
                .orElseThrow(() -> new BusinessException("Email chưa được đăng ký"));
        otpService.verify(user.getEmail(), form.getOtp(), OtpPurpose.RESET_PASSWORD);
        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
    }

    @Override
    public void changePassword(Long userId, ChangePasswordForm form) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("Tài khoản không tồn tại"));
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }
        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
