package vn.hcmute.rebook.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import vn.hcmute.rebook.dto.ProductFilter;
import vn.hcmute.rebook.dto.ProductForm;
import vn.hcmute.rebook.entity.Product;
import vn.hcmute.rebook.entity.enums.ProductStatus;

import java.util.List;

public interface ProductService {
    /** Tìm kiếm + lọc sách đang bán */
    Page<Product> search(ProductFilter filter);

    /** Trang chủ khách: sách đã bán trên 10 cuốn, giảm dần theo lượng bán */
    List<Product> guestHomeBestSellers();

    /** Tab trang chủ user: newest | bestseller | rating | favorite (20 sách / trang) */
    Page<Product> homeTab(String tab, int page);

    Product getVisibleProduct(Long id);

    /** Xem chi tiết: tăng lượt xem và ghi lại "sách đã xem" nếu đã đăng nhập */
    void recordView(Long productId, Long userId);

    List<Product> related(Product product);

    Page<Product> viewedBy(Long userId, int page);

    // ===== Vendor =====
    Page<Product> storeProducts(Long storeId, String keyword, ProductStatus status, int page);

    Product getStoreProduct(Long storeId, Long productId);

    ProductForm toForm(Product p);

    Product save(Long storeId, ProductForm form, MultipartFile cover, List<MultipartFile> gallery);

    void deleteImage(Long storeId, Long productId, Long imageId);

    void changeStatusByVendor(Long storeId, Long productId, ProductStatus status);

    // ===== Admin / Manager =====
    Page<Product> adminSearch(Long storeId, String keyword, ProductStatus status, int page);

    void changeStatusByAdmin(Long productId, ProductStatus status);
}
