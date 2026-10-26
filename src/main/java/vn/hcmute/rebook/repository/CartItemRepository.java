package vn.hcmute.rebook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hcmute.rebook.entity.CartItem;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("select c from CartItem c join fetch c.product p join fetch p.store where c.user.id = :userId order by p.store.id, c.id")
    List<CartItem> findByUserIdFetch(@Param("userId") Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(sum(c.quantity), 0) from CartItem c where c.user.id = :userId")
    int sumQuantity(@Param("userId") Long userId);
}
