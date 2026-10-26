package vn.hcmute.rebook.service;

import org.springframework.data.domain.Page;
import vn.hcmute.rebook.entity.Favorite;

public interface FavoriteService {
    /** @return true nếu sau thao tác sách đang được thích */
    boolean toggle(Long userId, Long productId);

    boolean isFavorite(Long userId, Long productId);

    Page<Favorite> list(Long userId, int page);
}
