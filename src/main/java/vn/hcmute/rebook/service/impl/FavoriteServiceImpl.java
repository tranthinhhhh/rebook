package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hcmute.rebook.entity.Favorite;
import vn.hcmute.rebook.entity.Product;
import vn.hcmute.rebook.exception.NotFoundException;
import vn.hcmute.rebook.repository.FavoriteRepository;
import vn.hcmute.rebook.repository.ProductRepository;
import vn.hcmute.rebook.repository.UserRepository;
import vn.hcmute.rebook.service.FavoriteService;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public boolean toggle(Long userId, Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new NotFoundException("Không tìm thấy sách"));
        var existing = favoriteRepository.findByUserIdAndProductId(userId, productId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            product.setFavoriteCount(Math.max(0, product.getFavoriteCount() - 1));
            return false;
        }
        favoriteRepository.save(Favorite.builder().user(userRepository.getReferenceById(userId)).product(product).build());
        product.setFavoriteCount(product.getFavoriteCount() + 1);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long productId) {
        return userId != null && favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Favorite> list(Long userId, int page) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), 20));
    }
}
