package vn.hcmute.rebook.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.hcmute.rebook.service.VnPayService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Tích hợp cổng thanh toán VNPay (môi trường sandbox), phiên bản API 2.1.0 */
@Service
public class VnPayServiceImpl implements VnPayService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Value("${app.vnpay.tmn-code}")
    private String tmnCode;
    @Value("${app.vnpay.hash-secret}")
    private String hashSecret;
    @Value("${app.vnpay.pay-url}")
    private String payUrl;
    @Value("${app.vnpay.return-url}")
    private String returnUrl;

    @Override
    public boolean isEnabled() {
        return !tmnCode.isBlank() && !hashSecret.isBlank();
    }

    @Override
    public String createPaymentUrl(String txnRef, long amount, String orderInfo, String clientIp) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", now.format(FMT));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(FMT));

        String query = buildQuery(params);
        return payUrl + "?" + query + "&vnp_SecureHash=" + hmacSha512(hashSecret, query);
    }

    @Override
    public boolean verifySignature(Map<String, String> input) {
        String received = input.get("vnp_SecureHash");
        if (received == null) return false;
        Map<String, String> params = new TreeMap<>(input);
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        return hmacSha512(hashSecret, buildQuery(params)).equalsIgnoreCase(received);
    }

    private String buildQuery(Map<String, String> sorted) {
        return sorted.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.US_ASCII) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được chữ ký VNPay", e);
        }
    }
}
