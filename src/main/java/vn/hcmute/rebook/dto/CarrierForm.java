package vn.hcmute.rebook.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CarrierForm {
    private Long id;

    @NotBlank(message = "Vui lòng nhập tên nhà vận chuyển")
    @Size(max = 100)
    private String name;

    @NotNull(message = "Vui lòng nhập phí vận chuyển")
    @Min(value = 0, message = "Phí không được âm")
    private Long fee;

    @Size(max = 50)
    private String estimatedTime;

    private boolean active = true;
}
