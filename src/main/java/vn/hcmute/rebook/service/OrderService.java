package vn.hcmute.rebook.service;

import org.springframework.data.domain.Page;
import vn.hcmute.rebook.dto.CheckoutForm;
import vn.hcmute.rebook.dto.CheckoutSummary;
import vn.hcmute.rebook.entity.Order;
import vn.hcmute.rebook.entity.enums.OrderStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {
    /** Tính tiền giỏ hàng: tách đơn theo shop, phí ship, mã giảm giá */
    CheckoutSummary preview(Long userId, Long carrierId, String voucherCode);

    /** Đặt hàng, trả về danh sách đơn đã tạo */
    List<Order> placeOrder(Long userId, CheckoutForm form);

    /** Xử lý kết quả VNPay trả về; trả về true nếu thanh toán thành công */
    boolean handleVnPayReturn(Map<String, String> params);

    List<Order> findByPaymentRef(String ref);

    // ===== User =====
    Page<Order> userOrders(Long userId, OrderStatus status, int page);

    Order getUserOrder(Long userId, Long orderId);

    void cancelByUser(Long userId, Long orderId);

    void returnByUser(Long userId, Long orderId);

    Map<OrderStatus, Long> countUserOrdersByStatus(Long userId);

    // ===== Vendor =====
    Page<Order> storeOrders(Long storeId, OrderStatus status, String keyword, int page);

    Order getStoreOrder(Long storeId, Long orderId);

    void updateByVendor(Long storeId, Long orderId, OrderStatus newStatus);

    void assignShipper(Long storeId, Long orderId, Long shipperId);

    Map<OrderStatus, Long> countStoreOrdersByStatus(Long storeId);

    // ===== Shipper =====
    Page<Order> shipperOrders(Long shipperId, OrderStatus status, int page);

    Order getShipperOrder(Long shipperId, Long orderId);

    void updateByShipper(Long shipperId, Long orderId, OrderStatus newStatus);

    // ===== Admin =====
    Page<Order> adminSearch(OrderStatus status, Long storeId, String keyword, int page);

    Order getById(Long id);
}
