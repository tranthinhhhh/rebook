package vn.hcmute.rebook.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import vn.hcmute.rebook.entity.enums.BookCondition;
import vn.hcmute.rebook.entity.enums.ProductStatus;

@Data
public class ProductForm {
    private Long id;

    @NotBlank(message = "Vui lòng nhập tên sách")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "Vui lòng nhập tác giả")
    @Size(max = 150)
    private String author;

    @Size(max = 150)
    private String publisher;

    @Min(value = 1900, message = "Năm xuất bản không hợp lệ")
    @Max(value = 2100, message = "Năm xuất bản không hợp lệ")
    private Integer publishYear;

    @Size(max = 20)
    private String isbn;

    @Min(value = 1, message = "Số trang phải lớn hơn 0")
    private Integer pages;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    @NotNull(message = "Vui lòng chọn tình trạng sách")
    private BookCondition bookCondition;

    @Size(max = 5000)
    private String description;

    @NotNull(message = "Vui lòng nhập giá bán")
    @Min(value = 1000, message = "Giá bán tối thiểu 1.000đ")
    private Long price;

    @Min(value = 0, message = "Giá bìa không hợp lệ")
    private Long originalPrice;

    @Min(value = 0, message = "Khuyến mãi từ 0 đến 90%")
    @Max(value = 90, message = "Khuyến mãi từ 0 đến 90%")
    private int discountPercent;

    @NotNull(message = "Vui lòng nhập số lượng")
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer stock;

    private ProductStatus status = ProductStatus.ACTIVE;
}
