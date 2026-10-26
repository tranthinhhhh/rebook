package vn.hcmute.rebook.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterForm {
    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(min = 2, max = 100, message = "Họ tên từ 2 đến 100 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 số, bắt đầu bằng 0")
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,50}$", message = "Mật khẩu tối thiểu 6 ký tự, gồm cả chữ và số")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu")
    private String confirmPassword;

    @AssertTrue(message = "Mật khẩu nhập lại không khớp")
    public boolean isPasswordMatched() {
        return password != null && password.equals(confirmPassword);
    }
}
