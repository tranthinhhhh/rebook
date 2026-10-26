package vn.hcmute.rebook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.hcmute.rebook.entity.ViewedProduct;

import java.util.Optional;

public interface ViewedProductRepository extends JpaRepository<ViewedProduct, Long> {
    Optional<ViewedProduct> findByUserIdAndProductId(Long userId, Long productId);

    Page<ViewedProduct> findByUserIdOrderByViewedAtDesc(Long userId, Pageable pageable);
}
