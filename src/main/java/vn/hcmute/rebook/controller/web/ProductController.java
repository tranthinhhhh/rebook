package vn.hcmute.rebook.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.hcmute.rebook.dto.ProductFilter;
import vn.hcmute.rebook.entity.Category;
import vn.hcmute.rebook.entity.Product;
import vn.hcmute.rebook.entity.Store;
import vn.hcmute.rebook.entity.enums.BookCondition;
import vn.hcmute.rebook.security.SecurityUtils;
import vn.hcmute.rebook.service.*;

@Controller
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final StoreService storeService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;

    /** Tìm kiếm và lọc sách */
    @GetMapping("/search")
    public String search(@ModelAttribute("filter") ProductFilter filter, Model model) {
        model.addAttribute("page", productService.search(filter));
        model.addAttribute("conditions", BookCondition.values());
        model.addAttribute("stores", storeService.findActiveStores());
        model.addAttribute("heading", filter.getQ() != null && !filter.getQ().isBlank()
                ? "Kết quả cho \"" + filter.getQ().trim() + "\"" : "Tất cả sách");
        return "product/search";
    }

    /** Trang sách theo danh mục */
    @GetMapping("/category/{slug}")
    public String category(@PathVariable String slug, @ModelAttribute("filter") ProductFilter filter, Model model) {
        Category category = categoryService.getBySlug(slug);
        filter.setCategoryId(category.getId());
        model.addAttribute("category", category);
        model.addAttribute("page", productService.search(filter));
        model.addAttribute("conditions", BookCondition.values());
        model.addAttribute("stores", storeService.findActiveStores());
        model.addAttribute("heading", category.getName());
        return "product/search";
    }

    @GetMapping("/books/{id}")
    public String detail(@PathVariable Long id, @RequestParam(defaultValue = "0") int reviewPage, Model model) {
        Product product = productService.getVisibleProduct(id);
        Long userId = SecurityUtils.currentUserId();
        productService.recordView(id, userId);
        model.addAttribute("p", product);
        model.addAttribute("related", productService.related(product));
        model.addAttribute("reviews", reviewService.productReviews(id, reviewPage));
        model.addAttribute("reviewable", reviewService.reviewableItems(userId, id));
        model.addAttribute("isFavorite", favoriteService.isFavorite(userId, id));
        return "product/detail";
    }

    @GetMapping("/stores")
    public String stores(Model model) {
        model.addAttribute("stores", storeService.findActiveStores());
        return "product/stores";
    }

    @GetMapping("/stores/{id}")
    public String store(@PathVariable Long id, @ModelAttribute("filter") ProductFilter filter, Model model) {
        Store store = storeService.getActiveStore(id);
        filter.setStoreId(id);
        model.addAttribute("store", store);
        model.addAttribute("page", productService.search(filter));
        return "product/store";
    }
}
