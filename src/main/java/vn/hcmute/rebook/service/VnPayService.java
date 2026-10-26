package vn.hcmute.rebook.service;

import java.util.Map;

public interface VnPayService {
    boolean isEnabled();

    String createPaymentUrl(String txnRef, long amount, String orderInfo, String clientIp);

    /** Kiểm tra chữ ký vnp_SecureHash của dữ liệu VNPay trả về */
    boolean verifySignature(Map<String, String> params);
}
