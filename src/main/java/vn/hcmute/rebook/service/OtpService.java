package vn.hcmute.rebook.service;

import vn.hcmute.rebook.entity.enums.OtpPurpose;

public interface OtpService {
    void generateAndSend(String email, OtpPurpose purpose);

    /** Kiểm tra OTP, ném BusinessException nếu sai/hết hạn */
    void verify(String email, String code, OtpPurpose purpose);
}
