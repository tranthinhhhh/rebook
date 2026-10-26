package vn.hcmute.rebook.entity.enums;

public enum ProductStatus {
    ACTIVE("Đang bán"), HIDDEN("Đang ẩn"), BANNED("Bị cấm");

    private final String label;
    ProductStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
