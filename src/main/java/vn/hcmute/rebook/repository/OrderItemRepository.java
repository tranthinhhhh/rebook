package vn.hcmute.rebook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hcmute.rebook.entity.OrderItem;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    /** Các dòng đơn đã giao của user cho 1 cuốn sách mà chưa đánh giá */
    @Query("select i from OrderItem i where i.order.user.id = :userId and i.product.id = :productId and i.order.status = vn.hcmute.rebook.entity.enums.OrderStatus.DELIVERED and i.reviewed = false")
    List<OrderItem> findReviewable(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("select i from OrderItem i where i.id = :id and i.order.user.id = :userId")
    Optional<OrderItem> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /** Top sách bán chạy của shop: [productTitle, tổng số lượng, doanh thu] */
    @Query("""
            select i.productTitle, sum(i.quantity), sum(i.price * i.quantity) from OrderItem i
            where i.order.store.id = :storeId and i.order.status = vn.hcmute.rebook.entity.enums.OrderStatus.DELIVERED
            group by i.productTitle order by sum(i.quantity) desc
            """)
    List<Object[]> topProductsOfStore(@Param("storeId") Long storeId, org.springframework.data.domain.Pageable pageable);
}
