package vn.hcmute.rebook.dto;

import vn.hcmute.rebook.entity.Store;

import java.util.List;

/** Kết quả tính tiền cho trang thanh toán (mỗi shop một đơn) */
public record CheckoutSummary(List<StoreLine> lines, long subtotal, long shippingFee, long discount, long total,
                              String voucherMessage) {
    public record StoreLine(Store store, CartGroup group, long subtotal, long shippingFee, long discount, long total) {}
}
