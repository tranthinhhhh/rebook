package vn.hcmute.rebook.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.hcmute.rebook.entity.Order;
import vn.hcmute.rebook.entity.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByCode(String code);

    List<Order> findByPaymentRef(String paymentRef);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatus(Long userId, OrderStatus status);

    // ===== Vendor =====
    @Query("""
            select o from Order o where o.store.id = :storeId
              and (:status is null or o.status = :status)
              and (:kw is null or o.code like concat('%', :kw, '%') or lower(o.receiverName) like lower(concat('%', :kw, '%')))
            order by o.createdAt desc
            """)
    Page<Order> searchByStore(@Param("storeId") Long storeId, @Param("status") OrderStatus status,
                              @Param("kw") String kw, Pageable pageable);

    Optional<Order> findByIdAndStoreId(Long id, Long storeId);

    long countByStoreIdAndStatus(Long storeId, OrderStatus status);

    @Query("select o from Order o where o.store.id = :storeId and o.status = vn.hcmute.rebook.entity.enums.OrderStatus.DELIVERED and o.deliveredAt >= :from order by o.deliveredAt")
    List<Order> findDeliveredByStoreSince(@Param("storeId") Long storeId, @Param("from") LocalDateTime from);

    // ===== Admin =====
    @Query("""
            select o from Order o where (:status is null or o.status = :status)
              and (:storeId is null or o.store.id = :storeId)
              and (:kw is null or o.code like concat('%', :kw, '%') or lower(o.receiverName) like lower(concat('%', :kw, '%')))
            order by o.createdAt desc
            """)
    Page<Order> adminSearch(@Param("status") OrderStatus status, @Param("storeId") Long storeId,
                            @Param("kw") String kw, Pageable pageable);

    @Query("select o from Order o where o.status = vn.hcmute.rebook.entity.enums.OrderStatus.DELIVERED and o.deliveredAt >= :from order by o.deliveredAt")
    List<Order> findDeliveredSince(@Param("from") LocalDateTime from);

    long countByStatus(OrderStatus status);

    // ===== Shipper =====
    @Query("""
            select o from Order o where o.shipper.id = :shipperId and (:status is null or o.status = :status)
            order by o.updatedAt desc
            """)
    Page<Order> findByShipper(@Param("shipperId") Long shipperId, @Param("status") OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndShipperId(Long id, Long shipperId);

    long countByShipperIdAndStatus(Long shipperId, OrderStatus status);

    @Query("select o from Order o where o.shipper.id = :shipperId and o.status = vn.hcmute.rebook.entity.enums.OrderStatus.DELIVERED and o.deliveredAt >= :from")
    List<Order> findDeliveredByShipperSince(@Param("shipperId") Long shipperId, @Param("from") LocalDateTime from);
}
