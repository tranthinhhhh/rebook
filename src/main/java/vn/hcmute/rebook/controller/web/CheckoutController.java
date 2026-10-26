package vn.hcmute.rebook.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.hcmute.rebook.dto.CartGroup;
import vn.hcmute.rebook.dto.CheckoutForm;
import vn.hcmute.rebook.entity.Address;
import vn.hcmute.rebook.entity.Carrier;
import vn.hcmute.rebook.entity.Order;
import vn.hcmute.rebook.entity.enums.PaymentMethod;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.security.SecurityUtils;
import vn.hcmute.rebook.service.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CheckoutController {
    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;
    private final CarrierService carrierService;
    private final VoucherService voucherService;
    private final VnPayService vnPayService;

    /** Trang thanh toán; tham số carrierId, voucherCode dùng để tính lại tiền */
    @GetMapping("/checkout")
    public String checkout(@ModelAttribute("form") CheckoutForm form, Model model, RedirectAttributes ra) {
        Long userId = SecurityUtils.currentUserId();
        List<Address> addresses = userService.getAddresses(userId);
        List<Carrier> carriers = carrierService.findActive();
        if (form.getAddressId() == null && !addresses.isEmpty()) form.setAddressId(addresses.get(0).getId());
        if (form.getCarrierId() == null && !carriers.isEmpty()) form.setCarrierId(carriers.get(0).getId());
        try {
            model.addAttribute("summary", orderService.preview(userId, form.getCarrierId(), form.getVoucherCode()));
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
        List<Long> storeIds = cartService.getGroups(userId).stream().map(CartGroup::store).map(s -> s.getId()).toList();
        model.addAttribute("addresses", addresses);
        model.addAttribute("carriers", carriers);
        model.addAttribute("vouchers", voucherService.findApplicable(storeIds));
        model.addAttribute("vnpayEnabled", vnPayService.isEnabled());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "user/checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@Valid @ModelAttribute("form") CheckoutForm form, BindingResult result,
                             HttpServletRequest request, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/checkout";
        }
        List<Order> orders = orderService.placeOrder(SecurityUtils.currentUserId(), form);
        String ref = orders.get(0).getPaymentRef();
        if (form.getPaymentMethod() == PaymentMethod.VNPAY) {
            long amount = orders.stream().mapToLong(Order::getTotal).sum();
            return "redirect:" + vnPayService.createPaymentUrl(ref, amount, "Thanh toan don hang ReBook " + ref, clientIp(request));
        }
        return "redirect:/checkout/success?ref=" + ref;
    }

    @GetMapping("/checkout/success")
    public String success(@RequestParam String ref, Model model) {
        Long userId = SecurityUtils.currentUserId();
        List<Order> orders = orderService.findByPaymentRef(ref).stream()
                .filter(o -> o.getUser().getId().equals(userId)).toList();
        model.addAttribute("orders", orders);
        model.addAttribute("total", orders.stream().mapToLong(Order::getTotal).sum());
        return "user/checkout-success";
    }

    /** VNPay chuyển hướng về đây sau khi khách thanh toán */
    @GetMapping("/payment/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> params, RedirectAttributes ra) {
        boolean ok = orderService.handleVnPayReturn(params);
        if (ok) ra.addFlashAttribute("success", "Thanh toán VNPay thành công!");
        else ra.addFlashAttribute("error", "Thanh toán VNPay không thành công, đơn hàng đã được hủy");
        return "redirect:/checkout/success?ref=" + params.getOrDefault("vnp_TxnRef", "");
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }
}
