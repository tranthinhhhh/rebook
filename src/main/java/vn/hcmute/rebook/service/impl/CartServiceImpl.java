package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hcmute.rebook.dto.CartGroup;
import vn.hcmute.rebook.entity.CartItem;
import vn.hcmute.rebook.entity.Product;
import vn.hcmute.rebook.entity.Store;
import vn.hcmute.rebook.entity.enums.ProductStatus;
import vn.hcmute.rebook.entity.enums.StoreStatus;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.exception.NotFoundException;
import vn.hcmute.rebook.repository.CartItemRepository;
import vn.hcmute.rebook.repository.ProductRepository;
import vn.hcmute.rebook.repository.StoreRepository;
import vn.hcmute.rebook.repository.UserRepository;
import vn.hcmute.rebook.service.CartService;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getItems(Long userId) {
        return cartItemRepository.findByUserIdFetch(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartGroup> getGroups(Long userId) {
        Map<Long, List<CartItem>> byStore = new LinkedHashMap<>();
        for (CartItem item : getItems(userId)) {
            byStore.computeIfAbsent(item.getProduct().getStore().getId(), k -> new ArrayList<>()).add(item);
        }
        List<CartGroup> groups = new ArrayList<>();
        byStore.forEach((storeId, items) -> groups.add(new CartGroup(items.get(0).getProduct().getStore(), items)));
        return groups;
    }

    @Override
    public void add(Long userId, Long productId, int quantity) {
        if (quantity < 1) throw new BusinessException("Số lượng không hợp lệ");
        Product product = productRepository.findById(productId).orElseThrow(() -> new NotFoundException("Không tìm thấy sách"));
        if (product.getStatus() != ProductStatus.ACTIVE || product.getStore().getStatus() != StoreStatus.ACTIVE) {
            throw new BusinessException("Sách đã ngừng bán");
        }
        Store own = storeRepository.findByOwnerId(userId).orElse(null);
        if (own != null && own.getId().equals(product.getStore().getId())) {
            throw new BusinessException("Bạn không thể mua sách của chính cửa hàng mình");
        }
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, productId).orElseGet(() ->
                CartItem.builder().user(userRepository.getReferenceById(userId)).product(product).quantity(0).build());
        int newQty = item.getQuantity() + quantity;
        if (newQty > product.getStock()) {
            throw new BusinessException("Chỉ còn " + product.getStock() + " cuốn trong kho"
                    + (item.getQuantity() > 0 ? " (giỏ hàng đã có " + item.getQuantity() + ")" : ""));
        }
        item.setQuantity(newQty);
        cartItemRepository.save(item);
    }

    @Override
    public void update(Long userId, Long cartItemId, int quantity) {
        CartItem item = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm trong giỏ"));
        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return;
        }
        if (quantity > item.getProduct().getStock()) {
            throw new BusinessException("Chỉ còn " + item.getProduct().getStock() + " cuốn \"" + item.getProduct().getTitle() + "\"");
        }
        item.setQuantity(quantity);
    }

    @Override
    public void remove(Long userId, Long cartItemId) {
        cartItemRepository.findByIdAndUserId(cartItemId, userId).ifPresent(cartItemRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public int count(Long userId) {
        return cartItemRepository.sumQuantity(userId);
    }

    @Override
    public void clear(Long userId) {
        cartItemRepository.deleteAll(cartItemRepository.findByUserIdFetch(userId));
    }
}
