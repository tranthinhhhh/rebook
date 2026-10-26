package vn.hcmute.rebook.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.hcmute.rebook.dto.ProductFilter;
import vn.hcmute.rebook.dto.ProductForm;
import vn.hcmute.rebook.entity.*;
import vn.hcmute.rebook.entity.enums.ProductStatus;
import vn.hcmute.rebook.entity.enums.StoreStatus;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.exception.NotFoundException;
import vn.hcmute.rebook.repository.*;
import vn.hcmute.rebook.service.NotificationService;
import vn.hcmute.rebook.service.ProductService;
import vn.hcmute.rebook.service.StorageService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {
    private static final int PAGE_SIZE = 20;
    private static final int MAX_GALLERY = 6;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final ProductImageRepository productImageRepository;
    private final ViewedProductRepository viewedProductRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<Product> search(ProductFilter f) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            ps.add(cb.equal(root.get("store").get("status"), StoreStatus.ACTIVE));
            if (f.getQ() != null && !f.getQ().isBlank()) {
                String like = "%" + f.getQ().trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("author")), like),
                        cb.like(cb.lower(root.get("publisher")), like),
                        cb.like(root.get("isbn"), like)));
            }
            if (f.getCategoryId() != null) ps.add(cb.equal(root.get("category").get("id"), f.getCategoryId()));
            if (f.getStoreId() != null) ps.add(cb.equal(root.get("store").get("id"), f.getStoreId()));
            if (f.getCondition() != null) ps.add(cb.equal(root.get("bookCondition"), f.getCondition()));
            if (f.getMinRating() != null) ps.add(cb.ge(root.get("ratingAvg"), f.getMinRating()));
            // Lọc theo giá sau khuyến mãi: price * (100 - discount) / 100
            var finalPrice = cb.quot(cb.prod(root.<Long>get("price"), cb.diff(100, root.<Integer>get("discountPercent"))), 100);
            if (f.getMinPrice() != null) ps.add(cb.ge(finalPrice.as(Long.class), f.getMinPrice()));
            if (f.getMaxPrice() != null) ps.add(cb.le(finalPrice.as(Long.class), f.getMaxPrice()));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        int size = f.getSize() <= 0 || f.getSize() > 60 ? PAGE_SIZE : f.getSize();
        return productRepository.findAll(spec, PageRequest.of(Math.max(f.getPage(), 0), size, sortOf(f.getSort())));
    }

    private Sort sortOf(String sort) {
        if (sort == null) sort = "newest";
        return switch (sort) {
            case "bestseller" -> Sort.by(Sort.Direction.DESC, "soldCount");
            case "rating" -> Sort.by(Sort.Direction.DESC, "ratingAvg", "ratingCount");
            case "favorite" -> Sort.by(Sort.Direction.DESC, "favoriteCount");
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.DESC, "createdAt", "id");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> guestHomeBestSellers() {
        return productRepository.findBestSellersAbove(ProductStatus.ACTIVE, StoreStatus.ACTIVE, 10, PageRequest.of(0, 20));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> homeTab(String tab, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, sortOf(tab));
        return productRepository.findVisible(ProductStatus.ACTIVE, StoreStatus.ACTIVE, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getVisibleProduct(Long id) {
        Product p = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy sách"));
        if (p.getStatus() != ProductStatus.ACTIVE || p.getStore().getStatus() != StoreStatus.ACTIVE) {
            throw new NotFoundException("Sách đã ngừng bán");
        }
        return p;
    }

    @Override
    public void recordView(Long productId, Long userId) {
        productRepository.increaseView(productId);
        if (userId == null) return;
        ViewedProduct v = viewedProductRepository.findByUserIdAndProductId(userId, productId).orElseGet(() ->
                ViewedProduct.builder()
                        .user(userRepository.getReferenceById(userId))
                        .product(productRepository.getReferenceById(productId))
                        .build());
        v.setViewedAt(LocalDateTime.now());
        viewedProductRepository.save(v);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> related(Product product) {
        return productRepository.findRelated(product.getCategory().getId(), product.getId(), PageRequest.of(0, 6));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> viewedBy(Long userId, int page) {
        return viewedProductRepository.findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(Math.max(page, 0), PAGE_SIZE))
                .map(ViewedProduct::getProduct);
    }

    // ===================== Vendor =====================

    @Override
    @Transactional(readOnly = true)
    public Page<Product> storeProducts(Long storeId, String keyword, ProductStatus status, int page) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return productRepository.searchInStore(storeId, kw, status,
                PageRequest.of(Math.max(page, 0), 15, Sort.by(Sort.Direction.DESC, "id")));
    }

    @Override
    @Transactional(readOnly = true)
    public Product getStoreProduct(Long storeId, Long productId) {
        return productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sách trong cửa hàng của bạn"));
    }

    @Override
    public ProductForm toForm(Product p) {
        ProductForm f = new ProductForm();
        f.setId(p.getId());
        f.setTitle(p.getTitle());
        f.setAuthor(p.getAuthor());
        f.setPublisher(p.getPublisher());
        f.setPublishYear(p.getPublishYear());
        f.setIsbn(p.getIsbn());
        f.setPages(p.getPages());
        f.setCategoryId(p.getCategory().getId());
        f.setBookCondition(p.getBookCondition());
        f.setDescription(p.getDescription());
        f.setPrice(p.getPrice());
        f.setOriginalPrice(p.getOriginalPrice());
        f.setDiscountPercent(p.getDiscountPercent());
        f.setStock(p.getStock());
        f.setStatus(p.getStatus() == ProductStatus.BANNED ? ProductStatus.HIDDEN : p.getStatus());
        return f;
    }

    @Override
    public Product save(Long storeId, ProductForm form, MultipartFile cover, List<MultipartFile> gallery) {
        Product p;
        if (form.getId() != null) {
            p = getStoreProduct(storeId, form.getId());
        } else {
            p = new Product();
            p.setStore(storeRepository.getReferenceById(storeId));
        }
        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new BusinessException("Danh mục không hợp lệ"));
        long original = form.getOriginalPrice() == null ? 0 : form.getOriginalPrice();
        if (original > 0 && form.getPrice() > original * 2) {
            throw new BusinessException("Giá sách cũ không nên cao hơn 2 lần giá bìa");
        }
        p.setTitle(form.getTitle().trim());
        p.setAuthor(form.getAuthor().trim());
        p.setPublisher(form.getPublisher());
        p.setPublishYear(form.getPublishYear());
        p.setIsbn(form.getIsbn());
        p.setPages(form.getPages());
        p.setCategory(category);
        p.setBookCondition(form.getBookCondition());
        p.setDescription(form.getDescription());
        p.setPrice(form.getPrice());
        p.setOriginalPrice(original);
        p.setDiscountPercent(form.getDiscountPercent());
        p.setStock(form.getStock());
        // Sách bị admin cấm thì vendor không tự mở lại được
        if (p.getStatus() != ProductStatus.BANNED) {
            p.setStatus(form.getStatus() == ProductStatus.HIDDEN ? ProductStatus.HIDDEN : ProductStatus.ACTIVE);
        }
        String coverUrl = storageService.upload(cover, "products");
        if (coverUrl != null) p.setCoverUrl(coverUrl);
        productRepository.save(p);

        if (gallery != null) {
            for (MultipartFile file : gallery) {
                if (file == null || file.isEmpty()) continue;
                if (p.getImages().size() >= MAX_GALLERY) throw new BusinessException("Tối đa " + MAX_GALLERY + " ảnh phụ");
                String url = storageService.upload(file, "products");
                p.getImages().add(ProductImage.builder().product(p).url(url).build());
            }
        }
        return p;
    }

    @Override
    public void deleteImage(Long storeId, Long productId, Long imageId) {
        Product p = getStoreProduct(storeId, productId);
        p.getImages().removeIf(img -> img.getId().equals(imageId));
    }

    @Override
    public void changeStatusByVendor(Long storeId, Long productId, ProductStatus status) {
        Product p = getStoreProduct(storeId, productId);
        if (p.getStatus() == ProductStatus.BANNED) throw new BusinessException("Sách đã bị quản trị viên cấm bán");
        if (status == ProductStatus.BANNED) throw new BusinessException("Trạng thái không hợp lệ");
        p.setStatus(status);
    }

    // ===================== Admin / Manager =====================

    @Override
    @Transactional(readOnly = true)
    public Page<Product> adminSearch(Long storeId, String keyword, ProductStatus status, int page) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return productRepository.adminSearch(storeId, kw, status,
                PageRequest.of(Math.max(page, 0), 15, Sort.by(Sort.Direction.DESC, "id")));
    }

    @Override
    public void changeStatusByAdmin(Long productId, ProductStatus status) {
        Product p = productRepository.findById(productId).orElseThrow(() -> new NotFoundException("Không tìm thấy sách"));
        p.setStatus(status);
        if (status == ProductStatus.BANNED) {
            notificationService.notify(p.getStore().getOwner(),
                    "Sách \"" + p.getTitle() + "\" đã bị quản trị viên cấm bán do vi phạm chính sách", "/vendor/products");
        }
    }
}
