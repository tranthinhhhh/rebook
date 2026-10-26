package vn.hcmute.rebook.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.hcmute.rebook.entity.enums.OrderStatus;
import vn.hcmute.rebook.security.SecurityUtils;
import vn.hcmute.rebook.service.OrderService;
import vn.hcmute.rebook.service.StatisticsService;

import java.util.List;

/** Kênh giao hàng (Shipper): đơn được phân công và thống kê */
@Controller
@RequestMapping("/shipper")
@RequiredArgsConstructor
public class ShipperController {
    private final OrderService orderService;
    private final StatisticsService statisticsService;

    @GetMapping
    public String dashboard(Model model) {
        Long id = SecurityUtils.currentUserId();
        model.addAttribute("summary", statisticsService.shipperSummary(id));
        model.addAttribute("chart", statisticsService.shipperDelivered(id));
        model.addAttribute("todo", orderService.shipperOrders(id, OrderStatus.SHIPPING, 0).getContent());
        return "shipper/dashboard";
    }

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) OrderStatus status, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", orderService.shipperOrders(SecurityUtils.currentUserId(), status, page));
        model.addAttribute("status", status);
        model.addAttribute("statuses", List.of(OrderStatus.CONFIRMED, OrderStatus.PICKED_UP, OrderStatus.SHIPPING,
                OrderStatus.DELIVERED, OrderStatus.CANCELLED));
        return "shipper/orders";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getShipperOrder(SecurityUtils.currentUserId(), id));
        return "shipper/order-detail";
    }

    @PostMapping("/orders/{id}/status")
    public String update(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes ra) {
        orderService.updateByShipper(SecurityUtils.currentUserId(), id, status);
        ra.addFlashAttribute("success", "Đã cập nhật đơn hàng: " + status.getLabel());
        return "redirect:/shipper/orders/" + id;
    }
}
