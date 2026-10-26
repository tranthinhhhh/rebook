package vn.hcmute.rebook.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.hcmute.rebook.security.SecurityUtils;
import vn.hcmute.rebook.service.ProductService;
import vn.hcmute.rebook.service.StoreService;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {
    /** Các tab trang chủ của User: mỗi tab 20 sách, có phân trang */
    private static final Map<String, String> TABS = Map.of(
            "newest", "Sách mới lên kệ", "bestseller", "Bán chạy nhất",
            "rating", "Đánh giá cao nhất", "favorite", "Được yêu thích nhất");

    private final ProductService productService;
    private final StoreService storeService;

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "newest") String tab,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("stores", storeService.findActiveStores());
        if (SecurityUtils.currentUser() == null) {
            // Guest: sách đã bán trên 10 cuốn, sắp xếp lượng bán giảm dần
            model.addAttribute("bestSellers", productService.guestHomeBestSellers());
            return "home/guest";
        }
        if (!TABS.containsKey(tab)) tab = "newest";
        model.addAttribute("tab", tab);
        model.addAttribute("tabs", List.of("newest", "bestseller", "rating", "favorite"));
        model.addAttribute("tabNames", TABS);
        model.addAttribute("page", productService.homeTab(tab, page));
        return "home/user";
    }

    @GetMapping("/error/403")
    public String forbidden() {
        return "error/403";
    }
}
