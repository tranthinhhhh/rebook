package vn.hcmute.rebook.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.hcmute.rebook.dto.ProductDto;
import vn.hcmute.rebook.security.SecurityUtils;
import vn.hcmute.rebook.service.CartService;
import vn.hcmute.rebook.service.OrderService;

import java.util.List;
import java.util.Map;

/** REST API giỏ hàng và đơn hàng của người dùng (yêu cầu JWT) */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CartApiController {
    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping("/cart")
    public List<Map<String, Object>> cart() {
        return cartService.getItems(SecurityUtils.currentUserId()).stream()
                .map(i -> Map.<String, Object>of("id", i.getId(), "quantity", i.getQuantity(),
                        "lineTotal", i.getLineTotal(), "product", ProductDto.of(i.getProduct())))
                .toList();
    }

    @PostMapping("/cart")
    public Map<String, Object> add(@RequestBody Map<String, Integer> body) {
        Long uid = SecurityUtils.currentUserId();
        cartService.add(uid, body.get("productId").longValue(), body.getOrDefault("quantity", 1));
        return Map.of("cartCount", cartService.count(uid));
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> orders(@RequestParam(defaultValue = "0") int page) {
        return orderService.userOrders(SecurityUtils.currentUserId(), null, page).getContent().stream()
                .map(o -> Map.<String, Object>of("id", o.getId(), "code", o.getCode(), "store", o.getStore().getName(),
                        "status", o.getStatus().getLabel(), "total", o.getTotal(), "createdAt", o.getCreatedAt().toString()))
                .toList();
    }
}
