package vn.hcmute.rebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StoreForm {
    @NotBlank(message = "Vui lòng nhập tên cửa hàng")
    @Size(min = 3, max = 120, message = "Tên cửa hàng từ 3 đến 120 ký tự")
    private String name;

    @Size(max = 2000)
    private String description;

    @NotBlank(message = "Vui lòng nhập địa chỉ cửa hàng")
    private String address;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 số, bắt đầu bằng 0")
    private String phone;
}
