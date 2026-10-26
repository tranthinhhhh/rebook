package vn.hcmute.rebook.exception;

/** Lỗi nghiệp vụ, thông điệp được hiển thị trực tiếp cho người dùng */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
