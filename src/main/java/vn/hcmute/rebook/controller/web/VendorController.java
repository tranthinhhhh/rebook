package vn.hcmute.rebook.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.hcmute.rebook.dto.ProductForm;
import vn.hcmute.rebook.dto.StoreForm;
import vn.hcmute.rebook.dto.VoucherForm;
import vn.hcmute.rebook.entity.Store;
import vn.hcmute.rebook.entity.enums.*;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.exception.NotFoundException;
import vn.hcmute.rebook.security.SecurityUtils;
import vn.hcmute.rebook.service.*;

import java.time.LocalDateTime;
import java.util.List;

/** Kênh người bán (Vendor): quản lý shop, sách, đơn hàng, khuyến mãi, doanh thu */
@Controller
@RequestMapping("/vendor")
@RequiredArgsConstructor
public class VendorController {
    private final StoreService storeService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final VoucherService voucherService;
    private final StatisticsService statisticsService;
    private final UserService userService;
    private final ChatService chatService;

    private Store myStore() {
        return storeService.findByOwner(SecurityUtils.currentUserId())
                .orElseThrow(() -> new NotFoundException("Bạn chưa có cửa hàng"));
    }

    // ===================== Đăng ký shop =====================

    @GetMapping("/register")
    public String registerPage(Model model) {
        var existing = storeService.findByOwner(SecurityUtils.currentUserId());
        if (existing.isPresent() && existing.get().getStatus() == StoreStatus.ACTIVE
                && SecurityUtils.currentUser().getRole() == Role.VENDOR) {
            return "redirect:/vendor";
        }
        model.addAttribute("store", existing.orElse(null));
        if (!model.containsAttribute("form")) model.addAttribute("form", new StoreForm());
        return "vendor/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") StoreForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("store", null);
            return "vendor/register";
        }
        storeService.register(SecurityUtils.currentUserId(), form);
        ra.addFlashAttribute("success", "Đã gửi đăng ký cửa hàng, vui lòng chờ quản trị viên duyệt");
        return "redirect:/vendor/register";
    }

    // ===================== Tổng quan =====================

    @GetMapping
    public String dashboard(Model model) {
        Store store = myStore();
        model.addAttribute("store", store);
        model.addAttribute("summary", statisticsService.storeSummary(store.getId()));
        model.addAttribute("counts", orderService.countStoreOrdersByStatus(store.getId()));
        model.addAttribute("chart", statisticsService.storeRevenue(store.getId(), "day"));
        model.addAttribute("topProducts", statisticsService.storeTopProducts(store.getId()));
        model.addAttribute("recentOrders", orderService.storeOrders(store.getId(), null, null, 0).getContent());
        return "vendor/dashboard";
    }

    // ===================== Trang chủ shop =====================

    @GetMapping("/store")
    public String storePage(Model model) {
        Store store = myStore();
        if (!model.containsAttribute("form")) {
            StoreForm f = new StoreForm();
            f.setName(store.getName());
            f.setDescription(store.getDescription());
            f.setAddress(store.getAddress());
            f.setPhone(store.getPhone());
            model.addAttribute("form", f);
        }
        model.addAttribute("store", store);
        return "vendor/store";
    }

    @PostMapping("/store")
    public String updateStore(@Valid @ModelAttribute("form") StoreForm form, BindingResult result,
                              @RequestParam(required = false) MultipartFile logo,
                              @RequestParam(required = false) MultipartFile banner, Model model, RedirectAttributes ra) {
        Store store = myStore();
        if (result.hasErrors()) {
            model.addAttribute("store", store);
            return "vendor/store";
        }
        storeService.update(store.getId(), form, logo, banner);
        ra.addFlashAttribute("success", "Đã cập nhật thông tin cửa hàng");
        return "redirect:/vendor/store";
    }

    // ===================== Sách =====================

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String q, @RequestParam(required = false) ProductStatus status,
                           @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", productService.storeProducts(myStore().getId(), q, status, page));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statuses", ProductStatus.values());
        return "vendor/products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        model.addAttribute("form", new ProductForm());
        return productForm(model, null);
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        var p = productService.getStoreProduct(myStore().getId(), id);
        model.addAttribute("form", productService.toForm(p));
        return productForm(model, p);
    }

    private String productForm(Model model, Object product) {
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("conditions", BookCondition.values());
        return "vendor/product-form";
    }

    @PostMapping("/products/save")
    public String saveProduct(@Valid @ModelAttribute("form") ProductForm form, BindingResult result,
                              @RequestParam(required = false) MultipartFile cover,
                              @RequestParam(required = false) List<MultipartFile> gallery, Model model, RedirectAttributes ra) {
        Long storeId = myStore().getId();
        if (result.hasErrors()) {
            return productForm(model, form.getId() == null ? null : productService.getStoreProduct(storeId, form.getId()));
        }
        try {
            var p = productService.save(storeId, form, cover, gallery);
            ra.addFlashAttribute("success", "Đã lưu sách \"" + p.getTitle() + "\"");
            return "redirect:/vendor/products";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return productForm(model, form.getId() == null ? null : productService.getStoreProduct(storeId, form.getId()));
        }
    }

    @PostMapping("/products/{id}/status")
    public String productStatus(@PathVariable Long id, @RequestParam ProductStatus status, RedirectAttributes ra) {
        productService.changeStatusByVendor(myStore().getId(), id, status);
        ra.addFlashAttribute("success", "Đã chuyển trạng thái sách sang \"" + status.getLabel() + "\"");
        return "redirect:/vendor/products";
    }

    @PostMapping("/products/{id}/images/{imageId}/delete")
    public String deleteImage(@PathVariable Long id, @PathVariable Long imageId, RedirectAttributes ra) {
        productService.deleteImage(myStore().getId(), id, imageId);
        ra.addFlashAttribute("success", "Đã xóa ảnh");
        return "redirect:/vendor/products/" + id + "/edit";
    }

    // ===================== Đơn hàng =====================

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) OrderStatus status, @RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "0") int page, Model model) {
        Long storeId = myStore().getId();
        model.addAttribute("page", orderService.storeOrders(storeId, status, q, page));
        model.addAttribute("counts", orderService.countStoreOrdersByStatus(storeId));
        model.addAttribute("status", status);
        model.addAttribute("q", q);
        model.addAttribute("statuses", OrderStatus.values());
        return "vendor/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getStoreOrder(myStore().getId(), id));
        model.addAttribute("shippers", userService.getActiveShippers());
        return "vendor/order-detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrder(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes ra) {
        orderService.updateByVendor(myStore().getId(), id, status);
        ra.addFlashAttribute("success", "Đơn hàng đã chuyển sang \"" + status.getLabel() + "\"");
        return "redirect:/vendor/orders/" + id;
    }

    @PostMapping("/orders/{id}/shipper")
    public String assignShipper(@PathVariable Long id, @RequestParam Long shipperId, RedirectAttributes ra) {
        orderService.assignShipper(myStore().getId(), id, shipperId);
        ra.addFlashAttribute("success", "Đã phân công shipper cho đơn hàng");
        return "redirect:/vendor/orders/" + id;
    }

    // ===================== Khuyến mãi =====================

    @GetMapping("/vouchers")
    public String vouchers(Model model) {
        model.addAttribute("vouchers", voucherService.findStoreVouchers(myStore().getId()));
        return "vendor/vouchers";
    }

    @GetMapping("/vouchers/new")
    public String newVoucher(Model model) {
        VoucherForm f = new VoucherForm();
        f.setStartAt(LocalDateTime.now().withSecond(0).withNano(0));
        f.setEndAt(f.getStartAt().plusDays(30));
        model.addAttribute("form", f);
        return voucherForm(model);
    }

    @GetMapping("/vouchers/{id}/edit")
    public String editVoucher(@PathVariable Long id, Model model) {
        var v = voucherService.getById(id);
        if (v.getStore() == null || !v.getStore().getId().equals(myStore().getId())) throw new NotFoundException("Không tìm thấy mã");
        model.addAttribute("form", voucherService.toForm(v));
        return voucherForm(model);
    }

    private String voucherForm(Model model) {
        model.addAttribute("types", VoucherType.values());
        model.addAttribute("action", "/vendor/vouchers/save");
        model.addAttribute("backUrl", "/vendor/vouchers");
        return "vendor/voucher-form";
    }

    @PostMapping("/vouchers/save")
    public String saveVoucher(@Valid @ModelAttribute("form") VoucherForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return voucherForm(model);
        try {
            voucherService.save(form, myStore().getId());
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return voucherForm(model);
        }
        ra.addFlashAttribute("success", "Đã lưu chương trình khuyến mãi");
        return "redirect:/vendor/vouchers";
    }

    @PostMapping("/vouchers/{id}/delete")
    public String deleteVoucher(@PathVariable Long id, RedirectAttributes ra) {
        voucherService.delete(id, myStore().getId());
        ra.addFlashAttribute("success", "Đã xóa / ngừng mã khuyến mãi");
        return "redirect:/vendor/vouchers";
    }

    // ===================== Doanh thu =====================

    @GetMapping("/revenue")
    public String revenue(@RequestParam(defaultValue = "month") String period, Model model) {
        Long storeId = myStore().getId();
        model.addAttribute("period", period);
        model.addAttribute("chart", statisticsService.storeRevenue(storeId, period));
        model.addAttribute("summary", statisticsService.storeSummary(storeId));
        model.addAttribute("topProducts", statisticsService.storeTopProducts(storeId));
        return "vendor/revenue";
    }

    // ===================== Chat =====================

    @GetMapping("/chat")
    public String chat(@RequestParam(required = false) Long customer, Model model) {
        Store store = myStore();
        model.addAttribute("store", store);
        model.addAttribute("conversations", chatService.conversationsOfStore(store.getId()));
        if (customer != null) {
            model.addAttribute("customer", userService.getById(customer));
            model.addAttribute("messages", chatService.history(SecurityUtils.currentUserId(), store.getId(), customer));
        }
        return "vendor/chat";
    }
}
