package vn.hcmute.rebook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import vn.hcmute.rebook.entity.enums.BookCondition;
import vn.hcmute.rebook.entity.enums.ProductStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Một đầu sách cũ được rao bán bởi một cửa hàng */
@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 150)
    private String author;

    @Column(length = 150)
    private String publisher;

    private Integer publishYear;

    @Column(length = 20)
    private String isbn;

    private Integer pages;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookCondition bookCondition;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Giá bán của shop (VNĐ) */
    private long price;

    /** Giá bìa gốc của sách (VNĐ) */
    private long originalPrice;

    /** Khuyến mãi % riêng của shop cho đầu sách */
    private int discountPercent;

    private int stock;
    private int soldCount;
    private int viewCount;
    private int favoriteCount;
    private double ratingAvg;
    private int ratingCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    private String coverUrl;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Giá sau khi áp khuyến mãi của shop */
    public long getFinalPrice() {
        return price * (100 - discountPercent) / 100;
    }

    public boolean isAvailable() {
        return status == ProductStatus.ACTIVE && stock > 0;
    }
}
