package vn.hcmute.rebook.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.security.SecurityUtils;
import vn.hcmute.rebook.service.CartService;

import java.util.Map;

/** Giỏ hàng được lưu trong database theo từng tài khoản */
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public String view(Model model) {
        var groups = cartService.getGroups(SecurityUtils.currentUserId());
        model.addAttribute("groups", groups);
        model.addAttribute("total", groups.stream().mapToLong(g -> g.getSubtotal()).sum());
        return "user/cart";
    }

    /** Thêm vào giỏ (gọi bằng AJAX) */
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> add(@RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity) {
        Long userId = SecurityUtils.currentUserId();
        try {
            cartService.add(userId, productId, quantity);
            return ResponseEntity.ok(Map.of("message", "Đã thêm vào giỏ hàng", "cartCount", cartService.count(userId)));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/update")
    public String update(@RequestParam Long itemId, @RequestParam int quantity, RedirectAttributes ra) {
        cartService.update(SecurityUtils.currentUserId(), itemId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Long itemId, RedirectAttributes ra) {
        cartService.remove(SecurityUtils.currentUserId(), itemId);
        ra.addFlashAttribute("success", "Đã xóa sách khỏi giỏ hàng");
        return "redirect:/cart";
    }
}
