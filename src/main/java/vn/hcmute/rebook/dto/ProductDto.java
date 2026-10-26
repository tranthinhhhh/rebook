package vn.hcmute.rebook.dto;

import vn.hcmute.rebook.entity.Product;

public record ProductDto(Long id, String title, String author, String category, String store,
                         long price, long finalPrice, int discountPercent, String condition,
                         int stock, int soldCount, double ratingAvg, String coverUrl) {
    public static ProductDto of(Product p) {
        return new ProductDto(p.getId(), p.getTitle(), p.getAuthor(), p.getCategory().getName(), p.getStore().getName(),
                p.getPrice(), p.getFinalPrice(), p.getDiscountPercent(), p.getBookCondition().getLabel(),
                p.getStock(), p.getSoldCount(), p.getRatingAvg(), p.getCoverUrl());
    }
}
