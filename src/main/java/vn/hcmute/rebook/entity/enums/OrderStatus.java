package vn.hcmute.rebook.entity.enums;

public enum OrderStatus {
    NEW("Đơn mới", "primary"),
    CONFIRMED("Đã xác nhận", "info"),
    PICKED_UP("Đã lấy hàng", "secondary"),
    SHIPPING("Đang giao", "warning"),
    DELIVERED("Đã giao", "success"),
    CANCELLED("Đã hủy", "danger"),
    RETURNED("Trả hàng - Hoàn tiền", "dark");

    private final String label;
    private final String color;
    OrderStatus(String label, String color) { this.label = label; this.color = color; }
    public String getLabel() { return label; }
    public String getColor() { return color; }
}
