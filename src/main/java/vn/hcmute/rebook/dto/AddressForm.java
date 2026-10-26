package vn.hcmute.rebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressForm {
    private Long id;

    @NotBlank(message = "Vui lòng nhập tên người nhận")
    @Size(max = 100)
    private String receiverName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 số, bắt đầu bằng 0")
    private String phone;

    @NotBlank(message = "Vui lòng nhập số nhà, tên đường")
    private String detail;

    @NotBlank(message = "Vui lòng nhập phường/xã")
    private String ward;

    @NotBlank(message = "Vui lòng nhập quận/huyện")
    private String district;

    @NotBlank(message = "Vui lòng nhập tỉnh/thành phố")
    private String province;

    private boolean defaultAddress;
}
