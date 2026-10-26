package vn.hcmute.rebook.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import vn.hcmute.rebook.entity.enums.VoucherType;

import java.time.LocalDateTime;

@Data
public class VoucherForm {
    private Long id;

    @NotBlank(message = "Vui lòng nhập mã")
    @Pattern(regexp = "^[A-Za-z0-9]{3,30}$", message = "Mã gồm 3-30 ký tự chữ hoặc số, không dấu")
    private String code;

    @NotBlank(message = "Vui lòng nhập tên chương trình")
    @Size(max = 150)
    private String name;

    @NotNull
    private VoucherType type = VoucherType.PRODUCT_PERCENT;

    @Min(value = 1, message = "Mức giảm từ 1 đến 100%")
    @Max(value = 100, message = "Mức giảm từ 1 đến 100%")
    private int percent = 10;

    @Min(value = 0)
    private long maxDiscount;

    @Min(value = 0)
    private long minOrder;

    @NotNull(message = "Vui lòng chọn ngày bắt đầu")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startAt;

    @NotNull(message = "Vui lòng chọn ngày kết thúc")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endAt;

    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    private int quantity = 100;

    private boolean active = true;

    @AssertTrue(message = "Ngày kết thúc phải sau ngày bắt đầu")
    public boolean isValidRange() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }
}
