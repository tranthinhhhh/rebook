package vn.hcmute.rebook.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.hcmute.rebook.dto.*;
import vn.hcmute.rebook.entity.Address;
import vn.hcmute.rebook.entity.OrderItem;
import vn.hcmute.rebook.entity.User;
import vn.hcmute.rebook.entity.enums.OrderStatus;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.security.SecurityUtils;
import vn.hcmute.rebook.service.*;

import java.util.Map;

/** Các chức năng của người mua (User) */
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthService authService;
    private final OrderService orderService;
    private final FavoriteService favoriteService;
    private final ProductService productService;
    private final ReviewService reviewService;
    private final NotificationService notificationService;
    private final ChatService chatService;
    private final StoreService storeService;

    private Long uid() {
        return SecurityUtils.currentUserId();
    }

    // ===================== Hồ sơ =====================

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = userService.getById(uid());
        if (!model.containsAttribute("profileForm")) {
            ProfileForm f = new ProfileForm();
            f.setFullName(user.getFullName());
            f.setPhone(user.getPhone());
            model.addAttribute("profileForm", f);
        }
        if (!model.containsAttribute("passwordForm")) model.addAttribute("passwordForm", new ChangePasswordForm());
        model.addAttribute("user", user);
        model.addAttribute("orderCounts", orderService.countUserOrdersByStatus(uid()));
        return "user/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult result,
                                @RequestParam(required = false) MultipartFile avatar, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("passwordForm", new ChangePasswordForm());
            model.addAttribute("user", userService.getById(uid()));
            model.addAttribute("orderCounts", orderService.countUserOrdersByStatus(uid()));
            return "user/profile";
        }
        userService.updateProfile(uid(), form, avatar);
        ra.addFlashAttribute("success", "Cập nhật thông tin thành công");
        return "redirect:/user/profile";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") ChangePasswordForm form, BindingResult result,
                                 Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                authService.changePassword(uid(), form);
                ra.addFlashAttribute("success", "Đổi mật khẩu thành công");
                return "redirect:/user/profile";
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        User user = userService.getById(uid());
        ProfileForm f = new ProfileForm();
        f.setFullName(user.getFullName());
        f.setPhone(user.getPhone());
        model.addAttribute("profileForm", f);
        model.addAttribute("user", user);
        model.addAttribute("orderCounts", orderService.countUserOrdersByStatus(uid()));
        model.addAttribute("openPassword", true);
        return "user/profile";
    }

    // ===================== Địa chỉ =====================

    @GetMapping("/addresses")
    public String addresses(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("addresses", userService.getAddresses(uid()));
        if (!model.containsAttribute("addressForm")) {
            AddressForm f = new AddressForm();
            if (edit != null) {
                Address a = userService.getAddress(uid(), edit);
                f.setId(a.getId());
                f.setReceiverName(a.getReceiverName());
                f.setPhone(a.getPhone());
                f.setDetail(a.getDetail());
                f.setWard(a.getWard());
                f.setDistrict(a.getDistrict());
                f.setProvince(a.getProvince());
                f.setDefaultAddress(a.isDefaultAddress());
            }
            model.addAttribute("addressForm", f);
        }
        return "user/addresses";
    }

    @PostMapping("/addresses")
    public String saveAddress(@Valid @ModelAttribute("addressForm") AddressForm form, BindingResult result,
                              @RequestParam(required = false) String back, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("addresses", userService.getAddresses(uid()));
            model.addAttribute("showForm", true);
            return "user/addresses";
        }
        userService.saveAddress(uid(), form);
        ra.addFlashAttribute("success", "Đã lưu địa chỉ");
        return "checkout".equals(back) ? "redirect:/checkout" : "redirect:/user/addresses";
    }

    @PostMapping("/addresses/{id}/delete")
    public String deleteAddress(@PathVariable Long id, RedirectAttributes ra) {
        userService.deleteAddress(uid(), id);
        ra.addFlashAttribute("success", "Đã xóa địa chỉ");
        return "redirect:/user/addresses";
    }

    @PostMapping("/addresses/{id}/default")
    public String defaultAddress(@PathVariable Long id, RedirectAttributes ra) {
        userService.setDefaultAddress(uid(), id);
        ra.addFlashAttribute("success", "Đã đặt làm địa chỉ mặc định");
        return "redirect:/user/addresses";
    }

    // ===================== Đơn mua =====================

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) OrderStatus status, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", orderService.userOrders(uid(), status, page));
        model.addAttribute("status", status);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("counts", orderService.countUserOrdersByStatus(uid()));
        return "user/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getUserOrder(uid(), id));
        return "user/order-detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        orderService.cancelByUser(uid(), id);
        ra.addFlashAttribute("success", "Đã hủy đơn hàng");
        return "redirect:/user/orders/" + id;
    }

    @PostMapping("/orders/{id}/return")
    public String returnOrder(@PathVariable Long id, RedirectAttributes ra) {
        orderService.returnByUser(uid(), id);
        ra.addFlashAttribute("success", "Đã gửi yêu cầu trả hàng, tiền sẽ được hoàn lại cho bạn");
        return "redirect:/user/orders/" + id;
    }

    // ===================== Yêu thích & đã xem =====================

    @PostMapping("/favorites/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleFavorite(@RequestParam Long productId) {
        return ResponseEntity.ok(Map.of("favorite", favoriteService.toggle(uid(), productId)));
    }

    @GetMapping("/favorites")
    public String favorites(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", favoriteService.list(uid(), page).map(f -> f.getProduct()));
        model.addAttribute("title", "Sách yêu thích");
        model.addAttribute("icon", "bi-heart");
        return "user/product-list";
    }

    @GetMapping("/viewed")
    public String viewed(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", productService.viewedBy(uid(), page));
        model.addAttribute("title", "Sách đã xem");
        model.addAttribute("icon", "bi-clock-history");
        return "user/product-list";
    }

    // ===================== Đánh giá =====================

    @GetMapping("/reviews/new")
    public String reviewForm(@RequestParam Long item, Model model) {
        OrderItem orderItem = reviewService.getReviewableItem(uid(), item);
        ReviewForm form = new ReviewForm();
        form.setOrderItemId(item);
        model.addAttribute("item", orderItem);
        model.addAttribute("form", form);
        return "user/review-form";
    }

    @PostMapping("/reviews")
    public String createReview(@Valid @ModelAttribute("form") ReviewForm form, BindingResult result,
                               @RequestParam(required = false) MultipartFile media, Model model, RedirectAttributes ra) {
        OrderItem item = reviewService.getReviewableItem(uid(), form.getOrderItemId());
        if (result.hasErrors()) {
            model.addAttribute("item", item);
            return "user/review-form";
        }
        reviewService.create(uid(), form, media);
        ra.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá!");
        return "redirect:/books/" + item.getProduct().getId() + "#reviews";
    }

    // ===================== Thông báo =====================

    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("notifications", notificationService.latest(uid(), 50));
        notificationService.markAllRead(uid());
        return "user/notifications";
    }

    // ===================== Chat với shop =====================

    @GetMapping("/chat")
    public String chat(@RequestParam(required = false) Long store, Model model) {
        model.addAttribute("conversations", chatService.conversationsOfCustomer(uid()));
        if (store != null) {
            var s = storeService.getActiveStore(store);
            if (s.getOwner().getId().equals(uid())) throw new BusinessException("Đây là cửa hàng của bạn");
            model.addAttribute("activeStore", s);
            model.addAttribute("messages", chatService.history(uid(), store, uid()));
        }
        return "user/chat";
    }
}
