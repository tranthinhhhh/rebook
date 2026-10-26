package vn.hcmute.rebook.entity.enums;

public enum VoucherType {
    PRODUCT_PERCENT("Giảm % tiền sách"), SHIPPING_PERCENT("Giảm % phí vận chuyển");

    private final String label;
    VoucherType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
