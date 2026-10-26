package vn.hcmute.rebook.entity.enums;

public enum Role {
    USER("Khách hàng"), VENDOR("Người bán"), SHIPPER("Shipper"), MANAGER("Quản lý"), ADMIN("Quản trị viên");

    private final String label;
    Role(String label) { this.label = label; }
    public String getLabel() { return label; }
}
