package vn.hcmute.rebook.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.hcmute.rebook.entity.enums.PaymentMethod;

@Data
public class CheckoutForm {
    @NotNull(message = "Vui lòng chọn địa chỉ nhận hàng")
    private Long addressId;

    @NotNull(message = "Vui lòng chọn đơn vị vận chuyển")
    private Long carrierId;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    private String voucherCode;

    @Size(max = 500)
    private String note;
}
