package vn.hcmute.rebook.dto;

import lombok.Data;
import vn.hcmute.rebook.entity.enums.BookCondition;

/** Tham số tìm kiếm và lọc sách */
@Data
public class ProductFilter {
    private String q;
    private Long categoryId;
    private Long storeId;
    private Long minPrice;
    private Long maxPrice;
    private BookCondition condition;
    private Integer minRating;
    /** newest | bestseller | rating | favorite | price_asc | price_desc */
    private String sort = "newest";
    private int page = 0;
    private int size = 20;
}
