package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hcmute.rebook.dto.ChartData;
import vn.hcmute.rebook.entity.Order;
import vn.hcmute.rebook.entity.Store;
import vn.hcmute.rebook.entity.User;
import vn.hcmute.rebook.entity.enums.OrderStatus;
import vn.hcmute.rebook.entity.enums.ProductStatus;
import vn.hcmute.rebook.entity.enums.Role;
import vn.hcmute.rebook.entity.enums.StoreStatus;
import vn.hcmute.rebook.repository.*;
import vn.hcmute.rebook.service.StatisticsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToLongFunction;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    /** Gom dữ liệu theo ngày (30 ngày), tháng (12 tháng) hoặc năm (5 năm) */
    private <T> ChartData bucket(List<T> data, Function<T, LocalDateTime> timeOf, ToLongFunction<T> valueOf, String period) {
        LinkedHashMap<String, Long> buckets = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt;
        switch (period == null ? "month" : period) {
            case "day" -> {
                fmt = DateTimeFormatter.ofPattern("dd/MM");
                for (int i = 29; i >= 0; i--) buckets.put(today.minusDays(i).format(fmt), 0L);
            }
            case "year" -> {
                fmt = DateTimeFormatter.ofPattern("yyyy");
                for (int i = 4; i >= 0; i--) buckets.put(today.minusYears(i).format(fmt), 0L);
            }
            default -> {
                fmt = DateTimeFormatter.ofPattern("MM/yyyy");
                for (int i = 11; i >= 0; i--) buckets.put(today.minusMonths(i).format(fmt), 0L);
            }
        }
        for (T item : data) {
            LocalDateTime t = timeOf.apply(item);
            if (t == null) continue;
            buckets.computeIfPresent(t.format(fmt), (k, v) -> v + valueOf.applyAsLong(item));
        }
        return new ChartData(new ArrayList<>(buckets.keySet()), new ArrayList<>(buckets.values()));
    }

    private LocalDateTime since(String period) {
        LocalDate today = LocalDate.now();
        return switch (period == null ? "month" : period) {
            case "day" -> today.minusDays(29).atStartOfDay();
            case "year" -> today.minusYears(4).withDayOfYear(1).atStartOfDay();
            default -> today.minusMonths(11).withDayOfMonth(1).atStartOfDay();
        };
    }

    /** Doanh thu thực nhận của shop = tiền sách - giảm giá của đơn - chiết khấu app */
    private long netRevenue(Order o) {
        long goods = o.getSubtotal() - o.getDiscount();
        return Math.round(goods * (100 - o.getStore().getCommissionRate()) / 100.0);
    }

    @Override
    public ChartData storeRevenue(Long storeId, String period) {
        return bucket(orderRepository.findDeliveredByStoreSince(storeId, since(period)),
                Order::getDeliveredAt, this::netRevenue, period);
    }

    @Override
    public Map<String, Object> storeSummary(Long storeId) {
        List<Order> all = orderRepository.findDeliveredByStoreSince(storeId, LocalDateTime.of(2000, 1, 1, 0, 0));
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Store store = storeRepository.findById(storeId).orElseThrow();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalRevenue", all.stream().mapToLong(this::netRevenue).sum());
        m.put("monthRevenue", all.stream().filter(o -> o.getDeliveredAt().isAfter(monthStart)).mapToLong(this::netRevenue).sum());
        m.put("commission", all.stream().mapToLong(o -> o.getSubtotal() - o.getDiscount() - netRevenue(o)).sum());
        m.put("deliveredOrders", (long) all.size());
        m.put("newOrders", orderRepository.countByStoreIdAndStatus(storeId, OrderStatus.NEW));
        m.put("products", productRepository.countByStoreId(storeId));
        m.put("commissionRate", store.getCommissionRate());
        return m;
    }

    @Override
    public List<Object[]> storeTopProducts(Long storeId) {
        return orderItemRepository.topProductsOfStore(storeId, PageRequest.of(0, 5));
    }

    @Override
    public ChartData systemRevenue(String period) {
        // Doanh thu toàn sàn = tổng giá trị đơn đã giao (GMV)
        return bucket(orderRepository.findDeliveredSince(since(period)), Order::getDeliveredAt, Order::getTotal, period);
    }

    @Override
    public ChartData newUsers(String period) {
        LocalDateTime from = since(period);
        List<User> users = userRepository.findAll().stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(from)).toList();
        return bucket(users, User::getCreatedAt, u -> 1L, period);
    }

    @Override
    public ChartData newStores(String period) {
        LocalDateTime from = since(period);
        List<Store> stores = storeRepository.findAll().stream().filter(s -> s.getCreatedAt() != null && s.getCreatedAt().isAfter(from)).toList();
        return bucket(stores, Store::getCreatedAt, s -> 1L, period);
    }

    @Override
    public Map<String, Object> systemSummary() {
        List<Order> delivered = orderRepository.findDeliveredSince(LocalDateTime.of(2000, 1, 1, 0, 0));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("gmv", delivered.stream().mapToLong(Order::getTotal).sum());
        m.put("commission", delivered.stream().mapToLong(o -> o.getSubtotal() - o.getDiscount() - netRevenue(o)).sum());
        m.put("users", userRepository.countByRole(Role.USER) + userRepository.countByRole(Role.VENDOR));
        m.put("stores", storeRepository.countByStatus(StoreStatus.ACTIVE));
        m.put("pendingStores", storeRepository.countByStatus(StoreStatus.PENDING));
        m.put("products", productRepository.count());
        m.put("orders", orderRepository.count());
        m.put("newOrders", orderRepository.countByStatus(OrderStatus.NEW));
        return m;
    }

    @Override
    public Map<String, Object> shipperSummary(Long shipperId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("waiting", orderRepository.countByShipperIdAndStatus(shipperId, OrderStatus.CONFIRMED));
        m.put("pickedUp", orderRepository.countByShipperIdAndStatus(shipperId, OrderStatus.PICKED_UP));
        m.put("shipping", orderRepository.countByShipperIdAndStatus(shipperId, OrderStatus.SHIPPING));
        m.put("delivered", orderRepository.countByShipperIdAndStatus(shipperId, OrderStatus.DELIVERED));
        m.put("failed", orderRepository.countByShipperIdAndStatus(shipperId, OrderStatus.CANCELLED));
        List<Order> delivered = orderRepository.findDeliveredByShipperSince(shipperId, LocalDate.now().withDayOfMonth(1).atStartOfDay());
        m.put("monthDelivered", (long) delivered.size());
        m.put("codCollected", delivered.stream().filter(o -> o.getPaymentMethod().name().equals("COD")).mapToLong(Order::getTotal).sum());
        return m;
    }

    @Override
    public ChartData shipperDelivered(Long shipperId) {
        return bucket(orderRepository.findDeliveredByShipperSince(shipperId, since("day")), Order::getDeliveredAt, o -> 1L, "day");
    }
}
