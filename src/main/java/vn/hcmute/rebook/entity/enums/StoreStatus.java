package vn.hcmute.rebook.entity.enums;

public enum StoreStatus {
    PENDING("Chờ duyệt"), ACTIVE("Đang hoạt động"), BLOCKED("Bị khóa");

    private final String label;
    StoreStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
