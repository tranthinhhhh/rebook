package vn.hcmute.rebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryForm {
    private Long id;

    @NotBlank(message = "Vui lòng nhập tên danh mục")
    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String icon = "bi-book";

    @Size(max = 255)
    private String description;
}
