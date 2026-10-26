package vn.hcmute.rebook.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.hcmute.rebook.dto.CarrierForm;
import vn.hcmute.rebook.dto.CategoryForm;
import vn.hcmute.rebook.dto.VoucherForm;
import vn.hcmute.rebook.entity.Carrier;
import vn.hcmute.rebook.entity.Category;
import vn.hcmute.rebook.entity.User;
import vn.hcmute.rebook.entity.enums.*;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.security.CustomUserDetails;
import vn.hcmute.rebook.security.SecurityUtils;
import vn.hcmute.rebook.service.*;

import java.time.LocalDateTime;

/**
 * Trang quản trị dùng chung cho Admin và Manager.
 * Riêng phân quyền tài khoản (/admin/accounts/**) chỉ Admin được dùng (cấu hình trong SecurityConfig).
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final StatisticsService statisticsService;
    private final UserService userService;
    private final StoreService storeService;
    private final ProductService productService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final VoucherService voucherService;
    private final CarrierService carrierService;

    // ===================== Tổng quan =====================

    @GetMapping
    public String dashboard(@RequestParam(defaultValue = "month") String period, Model model) {
        model.addAttribute("period", period);
        model.addAttribute("summary", statisticsService.systemSummary());
        model.addAttribute("revenue", statisticsService.systemRevenue(period));
        model.addAttribute("newUsers", statisticsService.newUsers(period));
        model.addAttribute("newStores", statisticsService.newStores(period));
        model.addAttribute("recentOrders", orderService.adminSearch(null, null, null, 0).getContent());
        return "admin/dashboard";
    }

    // ===================== Người dùng =====================

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String q, @RequestParam(required = false) Role role,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", userService.search(q, role, page));
        model.addAttribute("q", q);
        model.addAttribute("role", role);
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/lock")
    public String lockUser(@PathVariable Long id, @RequestParam boolean locked, RedirectAttributes ra) {
        CustomUserDetails actor = SecurityUtils.currentUser();
        User target = userService.getById(id);
        if (actor.getRole() == Role.MANAGER && (target.getRole() == Role.MANAGER || target.getRole() == Role.ADMIN)) {
            throw new BusinessException("Quản lý không thể khóa tài khoản quản lý/quản trị khác");
        }
        userService.setLocked(id, locked, actor.getId());
        ra.addFlashAttribute("success", (locked ? "Đã khóa" : "Đã mở khóa") + " tài khoản " + target.getEmail());
        return "redirect:/admin/users";
    }

    /** Phân quyền: chỉ ADMIN */
    @PostMapping("/accounts/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam Role role, RedirectAttributes ra) {
        userService.changeRole(id, role, SecurityUtils.currentUserId());
        ra.addFlashAttribute("success", "Đã cấp quyền " + role.getLabel());
        return "redirect:/admin/users";
    }

    // ===================== Cửa hàng & chiết khấu =====================

    @GetMapping("/stores")
    public String stores(@RequestParam(required = false) String q, @RequestParam(required = false) StoreStatus status,
                         @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", storeService.search(q, status, page));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statuses", StoreStatus.values());
        return "admin/stores";
    }

    @PostMapping("/stores/{id}/status")
    public String storeStatus(@PathVariable Long id, @RequestParam StoreStatus status, RedirectAttributes ra) {
        storeService.changeStatus(id, status);
        ra.addFlashAttribute("success", "Cửa hàng đã chuyển sang \"" + status.getLabel() + "\"");
        return "redirect:/admin/stores";
    }

    @PostMapping("/stores/{id}/commission")
    public String commission(@PathVariable Long id, @RequestParam double rate, RedirectAttributes ra) {
        storeService.updateCommission(id, rate);
        ra.addFlashAttribute("success", "Đã cập nhật chiết khấu " + rate + "%");
        return "redirect:/admin/stores";
    }

    // ===================== Sách của các shop =====================

    @GetMapping("/products")
    public String products(@RequestParam(required = false) Long storeId, @RequestParam(required = false) String q,
                           @RequestParam(required = false) ProductStatus status, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", productService.adminSearch(storeId, q, status, page));
        model.addAttribute("stores", storeService.search(null, null, 0).getContent());
        model.addAttribute("storeId", storeId);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statuses", ProductStatus.values());
        return "admin/products";
    }

    @PostMapping("/products/{id}/status")
    public String productStatus(@PathVariable Long id, @RequestParam ProductStatus status, RedirectAttributes ra) {
        productService.changeStatusByAdmin(id, status);
        ra.addFlashAttribute("success", status == ProductStatus.BANNED ? "Đã cấm bán sách" : "Đã mở lại sách");
        return "redirect:/admin/products";
    }

    // ===================== Đơn hàng =====================

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) OrderStatus status, @RequestParam(required = false) Long storeId,
                         @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", orderService.adminSearch(status, storeId, q, page));
        model.addAttribute("stores", storeService.search(null, null, 0).getContent());
        model.addAttribute("status", status);
        model.addAttribute("storeId", storeId);
        model.addAttribute("q", q);
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getById(id));
        return "admin/order-detail";
    }

    // ===================== Danh mục =====================

    @GetMapping("/categories")
    public String categories(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("service", categoryService);
        if (!model.containsAttribute("form")) {
            CategoryForm f = new CategoryForm();
            if (edit != null) {
                Category c = categoryService.getById(edit);
                f.setId(c.getId());
                f.setName(c.getName());
                f.setIcon(c.getIcon());
                f.setDescription(c.getDescription());
            }
            model.addAttribute("form", f);
        }
        return "admin/categories";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@Valid @ModelAttribute("form") CategoryForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("service", categoryService);
            return "admin/categories";
        }
        categoryService.save(form);
        ra.addFlashAttribute("success", "Đã lưu danh mục " + form.getName());
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        categoryService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa danh mục");
        return "redirect:/admin/categories";
    }

    // ===================== Khuyến mãi toàn sàn =====================

    @GetMapping("/vouchers")
    public String vouchers(Model model) {
        model.addAttribute("vouchers", voucherService.findSystemVouchers());
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
        if (v.getStore() != null) throw new BusinessException("Đây là mã của cửa hàng " + v.getStore().getName());
        model.addAttribute("form", voucherService.toForm(v));
        return voucherForm(model);
    }

    private String voucherForm(Model model) {
        model.addAttribute("types", VoucherType.values());
        model.addAttribute("action", "/admin/vouchers/save");
        model.addAttribute("backUrl", "/admin/vouchers");
        return "vendor/voucher-form";
    }

    @PostMapping("/vouchers/save")
    public String saveVoucher(@Valid @ModelAttribute("form") VoucherForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return voucherForm(model);
        try {
            voucherService.save(form, null);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return voucherForm(model);
        }
        ra.addFlashAttribute("success", "Đã lưu khuyến mãi toàn sàn");
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/vouchers/{id}/delete")
    public String deleteVoucher(@PathVariable Long id, RedirectAttributes ra) {
        voucherService.delete(id, null);
        ra.addFlashAttribute("success", "Đã xóa / ngừng mã khuyến mãi");
        return "redirect:/admin/vouchers";
    }

    // ===================== Nhà vận chuyển =====================

    @GetMapping("/carriers")
    public String carriers(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("carriers", carrierService.findAll());
        if (!model.containsAttribute("form")) {
            CarrierForm f = new CarrierForm();
            if (edit != null) {
                Carrier c = carrierService.getById(edit);
                f.setId(c.getId());
                f.setName(c.getName());
                f.setFee(c.getFee());
                f.setEstimatedTime(c.getEstimatedTime());
                f.setActive(c.isActive());
            }
            model.addAttribute("form", f);
        }
        return "admin/carriers";
    }

    @PostMapping("/carriers/save")
    public String saveCarrier(@Valid @ModelAttribute("form") CarrierForm form, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("carriers", carrierService.findAll());
            return "admin/carriers";
        }
        carrierService.save(form);
        ra.addFlashAttribute("success", "Đã lưu nhà vận chuyển " + form.getName());
        return "redirect:/admin/carriers";
    }

    @PostMapping("/carriers/{id}/delete")
    public String deleteCarrier(@PathVariable Long id, RedirectAttributes ra) {
        carrierService.delete(id);
        ra.addFlashAttribute("success", "Đã xóa nhà vận chuyển");
        return "redirect:/admin/carriers";
    }
}
