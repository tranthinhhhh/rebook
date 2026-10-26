package vn.hcmute.rebook.service;

import vn.hcmute.rebook.dto.CartGroup;
import vn.hcmute.rebook.entity.CartItem;

import java.util.List;

public interface CartService {
    List<CartItem> getItems(Long userId);

    List<CartGroup> getGroups(Long userId);

    void add(Long userId, Long productId, int quantity);

    void update(Long userId, Long cartItemId, int quantity);

    void remove(Long userId, Long cartItemId);

    int count(Long userId);

    void clear(Long userId);
}
