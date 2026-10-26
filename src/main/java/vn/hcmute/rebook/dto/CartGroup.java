package vn.hcmute.rebook.dto;

import vn.hcmute.rebook.entity.CartItem;
import vn.hcmute.rebook.entity.Store;

import java.util.List;

/** Các sản phẩm trong giỏ được nhóm theo cửa hàng */
public record CartGroup(Store store, List<CartItem> items) {
    public long getSubtotal() {
        return items.stream().mapToLong(CartItem::getLineTotal).sum();
    }
}
