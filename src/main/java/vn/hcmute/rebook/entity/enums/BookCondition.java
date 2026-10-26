package vn.hcmute.rebook.entity.enums;

public enum BookCondition {
    LIKE_NEW("Như mới (95%+)"), GOOD("Tốt (85%)"), FAIR("Khá (70%)"), OLD("Cũ (dưới 70%)");

    private final String label;
    BookCondition(String label) { this.label = label; }
    public String getLabel() { return label; }
}
