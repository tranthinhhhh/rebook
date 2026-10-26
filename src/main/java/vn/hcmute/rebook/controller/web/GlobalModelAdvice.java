package vn.hcmute.rebook.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.hcmute.rebook.security.CustomUserDetails;
import vn.hcmute.rebook.security.SecurityUtils;
import vn.hcmute.rebook.service.CartService;
import vn.hcmute.rebook.service.CategoryService;
import vn.hcmute.rebook.service.NotificationService;

/** Thêm dữ liệu dùng chung (header, menu) cho mọi trang giao diện */
@ControllerAdvice(basePackages = "vn.hcmute.rebook.controller.web")
@RequiredArgsConstructor
public class GlobalModelAdvice {
    private final CartService cartService;
    private final CategoryService categoryService;
    private final NotificationService notificationService;

    @ModelAttribute
    public void addCommon(Model model, HttpServletRequest request) {
        CustomUserDetails user = SecurityUtils.currentUser();
        model.addAttribute("currentUser", user);
        model.addAttribute("currentUri", request.getRequestURI());
        model.addAttribute("navCategories", categoryService.findAll());
        // Chỉ hiện danh sách tài khoản demo khi chạy trên máy (không lộ khi chia sẻ link công khai)
        String host = request.getServerName();
        model.addAttribute("showDemoAccounts", "localhost".equals(host) || "127.0.0.1".equals(host));
        if (user != null) {
            model.addAttribute("cartCount", cartService.count(user.getId()));
            model.addAttribute("unreadCount", notificationService.countUnread(user.getId()));
            model.addAttribute("latestNotifications", notificationService.latest(user.getId(), 8));
        } else {
            model.addAttribute("cartCount", 0);
            model.addAttribute("unreadCount", 0L);
        }
    }
}
