package vn.hcmute.rebook.service;

import vn.hcmute.rebook.dto.ChangePasswordForm;
import vn.hcmute.rebook.dto.RegisterForm;
import vn.hcmute.rebook.dto.ResetPasswordForm;

public interface AuthService {
    void register(RegisterForm form);

    void activate(String email, String otp);

    void resendRegisterOtp(String email);

    void sendResetOtp(String email);

    void resetPassword(ResetPasswordForm form);

    void changePassword(Long userId, ChangePasswordForm form);
}
