package vn.hcmute.rebook.service;

public interface MailService {
    void sendOtp(String to, String code, String purposeLabel);
}
