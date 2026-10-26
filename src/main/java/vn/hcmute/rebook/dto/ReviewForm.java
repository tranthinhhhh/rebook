package vn.hcmute.rebook.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewForm {
    @NotNull
    private Long orderItemId;

    @Min(value = 1, message = "Vui lòng chọn số sao")
    @Max(5)
    private int rating = 5;

    @Size(min = 50, max = 2000, message = "Nội dung đánh giá tối thiểu 50 ký tự")
    private String content;
}
