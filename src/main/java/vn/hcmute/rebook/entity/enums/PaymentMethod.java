package vn.hcmute.rebook.entity.enums;

public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng"), VNPAY("VNPay");

    private final String label;
    PaymentMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}
