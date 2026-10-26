package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hcmute.rebook.dto.CartGroup;
import vn.hcmute.rebook.dto.CheckoutForm;
import vn.hcmute.rebook.dto.CheckoutSummary;
import vn.hcmute.rebook.entity.*;
import vn.hcmute.rebook.entity.enums.*;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.exception.NotFoundException;
import vn.hcmute.rebook.repository.*;
import vn.hcmute.rebook.service.*;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {
    private static final int PAGE_SIZE = 10;
    private static final int RETURN_DAYS = 7;
    private final SecureRandom random = new SecureRandom();

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final CarrierRepository carrierRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final NotificationService notificationService;
    private final VnPayService vnPayService;

    // ============================ Đặt hàng ============================

    @Override
    @Transactional(readOnly = true)
    public CheckoutSummary preview(Long userId, Long carrierId, String voucherCode) {
        List<CartGroup> groups = cartService.getGroups(userId);
        if (groups.isEmpty()) throw new BusinessException("Giỏ hàng đang trống");
        for (CartGroup g : groups) {
            if (g.store().getStatus() != StoreStatus.ACTIVE) {
                throw new BusinessException("Cửa hàng \"" + g.store().getName() + "\" đã ngừng hoạt động, vui lòng xóa sách của shop khỏi giỏ");
            }
            for (CartItem item : g.items()) {
                Product p = item.getProduct();
                if (p.getStatus() != ProductStatus.ACTIVE) {
                    throw new BusinessException("Sách \"" + p.getTitle() + "\" đã ngừng bán, vui lòng xóa khỏi giỏ");
                }
                if (item.getQuantity() > p.getStock()) {
                    throw new BusinessException("Sách \"" + p.getTitle() + "\" chỉ còn " + p.getStock() + " cuốn");
                }
            }
        }
        long fee = 0;
        if (carrierId != null) {
            Carrier carrier = carrierRepository.findById(carrierId).filter(Carrier::isActive)
                    .orElseThrow(() -> new BusinessException("Đơn vị vận chuyển không hợp lệ"));
            fee = carrier.getFee();
        }

        Voucher voucher = null;
        String message = null;
        long cartSubtotal = groups.stream().mapToLong(CartGroup::getSubtotal).sum();
        if (voucherCode != null && !voucherCode.isBlank()) {
            voucher = voucherRepository.findByCodeIgnoreCase(voucherCode.trim()).orElse(null);
            if (voucher == null || !voucher.isUsable()) {
                message = "Mã giảm giá không tồn tại hoặc đã hết hạn";
                voucher = null;
            } else if (voucher.getStore() != null && !hasStore(groups, voucher.getStore().getId())) {
                message = "Mã chỉ áp dụng cho sách của cửa hàng " + voucher.getStore().getName();
                voucher = null;
            }
        }

        List<CheckoutSummary.StoreLine> lines = new ArrayList<>();
        long totalDiscount = 0;
        for (CartGroup g : groups) {
            long sub = g.getSubtotal();
            long discount = 0;
            if (voucher != null && (voucher.getStore() == null || voucher.getStore().getId().equals(g.store().getId()))) {
                long base = voucher.getStore() == null ? cartSubtotal : sub;
                if (base >= voucher.getMinOrder()) {
                    discount = voucher.getType() == VoucherType.PRODUCT_PERCENT
                            ? sub * voucher.getPercent() / 100
                            : fee * voucher.getPercent() / 100;
                    if (voucher.getMaxDiscount() > 0) discount = Math.min(discount, voucher.getMaxDiscount());
                }
            }
            totalDiscount += discount;
            lines.add(new CheckoutSummary.StoreLine(g.store(), g, sub, fee, discount, sub + fee - discount));
        }
        if (voucher != null) {
            message = totalDiscount > 0
                    ? "Áp dụng mã " + voucher.getCode() + " thành công"
                    : "Đơn hàng chưa đạt giá trị tối thiểu để dùng mã " + voucher.getCode();
        }
        long shippingTotal = fee * groups.size();
        return new CheckoutSummary(lines, cartSubtotal, shippingTotal, totalDiscount,
                cartSubtotal + shippingTotal - totalDiscount, message);
    }

    private boolean hasStore(List<CartGroup> groups, Long storeId) {
        return groups.stream().anyMatch(g -> g.store().getId().equals(storeId));
    }

    @Override
    public List<Order> placeOrder(Long userId, CheckoutForm form) {
        if (form.getPaymentMethod() == PaymentMethod.VNPAY && !vnPayService.isEnabled()) {
            throw new BusinessException("VNPay chưa được cấu hình, vui lòng chọn thanh toán khi nhận hàng");
        }
        Address address = addressRepository.findByIdAndUserId(form.getAddressId(), userId)
                .orElseThrow(() -> new BusinessException("Địa chỉ nhận hàng không hợp lệ"));
        Carrier carrier = carrierRepository.findById(form.getCarrierId()).filter(Carrier::isActive)
                .orElseThrow(() -> new BusinessException("Đơn vị vận chuyển không hợp lệ"));
        CheckoutSummary summary = preview(userId, carrier.getId(), form.getVoucherCode());
        User user = userRepository.getReferenceById(userId);
        String paymentRef = "PAY" + System.currentTimeMillis() + userId;

        List<Order> orders = new ArrayList<>();
        for (CheckoutSummary.StoreLine line : summary.lines()) {
            Order order = Order.builder()
                    .code(newOrderCode())
                    .user(user)
                    .store(line.store())
                    .receiverName(address.getReceiverName())
                    .receiverPhone(address.getPhone())
                    .shippingAddress(address.getFullAddress())
                    .carrier(carrier)
                    .status(OrderStatus.NEW)
                    .paymentMethod(form.getPaymentMethod())
                    .paymentStatus(PaymentStatus.UNPAID)
                    .paymentRef(paymentRef)
                    .voucherCode(line.discount() > 0 ? form.getVoucherCode().trim().toUpperCase() : null)
                    .subtotal(line.subtotal())
                    .shippingFee(line.shippingFee())
                    .discount(line.discount())
                    .total(line.total())
                    .note(form.getNote())
                    .build();
            for (CartItem ci : line.group().items()) {
                Product p = ci.getProduct();
                if (ci.getQuantity() > p.getStock()) {
                    throw new BusinessException("Sách \"" + p.getTitle() + "\" vừa hết hàng");
                }
                p.setStock(p.getStock() - ci.getQuantity());
                order.getItems().add(OrderItem.builder()
                        .order(order).product(p).productTitle(p.getTitle())
                        .price(p.getFinalPrice()).quantity(ci.getQuantity())
                        .build());
            }
            orders.add(orderRepository.save(order));
        }
        if (summary.discount() > 0) {
            voucherRepository.findByCodeIgnoreCase(form.getVoucherCode().trim())
                    .ifPresent(v -> v.setUsedCount(v.getUsedCount() + 1));
        }
        cartService.clear(userId);
        for (Order o : orders) {
            notificationService.notify(o.getStore().getOwner(),
                    "Đơn hàng mới #" + o.getCode() + " - " + o.getItems().size() + " đầu sách", "/vendor/orders/" + o.getId());
        }
        return orders;
    }

    private String newOrderCode() {
        String prefix = "RB" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String code;
        do {
            code = prefix + String.format("%05d", random.nextInt(100_000));
        } while (orderRepository.findByCode(code).isPresent());
        return code;
    }

    @Override
    public boolean handleVnPayReturn(Map<String, String> params) {
        if (!vnPayService.verifySignature(params)) {
            log.warn("Sai chữ ký VNPay: {}", params);
            return false;
        }
        List<Order> orders = orderRepository.findByPaymentRef(params.get("vnp_TxnRef"));
        if (orders.isEmpty()) return false;
        long expected = orders.stream().mapToLong(Order::getTotal).sum() * 100;
        boolean success = "00".equals(params.get("vnp_ResponseCode"))
                && String.valueOf(expected).equals(params.get("vnp_Amount"));
        for (Order o : orders) {
            if (o.getPaymentStatus() != PaymentStatus.UNPAID || o.getStatus() != OrderStatus.NEW) continue;
            if (success) {
                o.setPaymentStatus(PaymentStatus.PAID);
            } else {
                cancelInternal(o, "Thanh toán VNPay không thành công");
            }
        }
        return success;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByPaymentRef(String ref) {
        return orderRepository.findByPaymentRef(ref);
    }

    // ============================ User ============================

    @Override
    @Transactional(readOnly = true)
    public Page<Order> userOrders(Long userId, OrderStatus status, int page) {
        var pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        return status == null
                ? orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getUserOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
    }

    @Override
    public void cancelByUser(Long userId, Long orderId) {
        Order o = getUserOrder(userId, orderId);
        if (o.getStatus() != OrderStatus.NEW && o.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("Chỉ được hủy đơn khi shop chưa giao cho đơn vị vận chuyển");
        }
        cancelInternal(o, "Khách hàng hủy đơn");
        notificationService.notify(o.getStore().getOwner(), "Khách hàng đã hủy đơn #" + o.getCode(), "/vendor/orders/" + o.getId());
    }

    @Override
    public void returnByUser(Long userId, Long orderId) {
        Order o = getUserOrder(userId, orderId);
        if (o.getStatus() != OrderStatus.DELIVERED) throw new BusinessException("Chỉ được trả hàng với đơn đã giao");
        if (o.getDeliveredAt() != null && o.getDeliveredAt().plusDays(RETURN_DAYS).isBefore(LocalDateTime.now())) {
            throw new BusinessException("Đã quá " + RETURN_DAYS + " ngày kể từ khi nhận hàng, không thể trả hàng");
        }
        o.setStatus(OrderStatus.RETURNED);
        o.setPaymentStatus(PaymentStatus.REFUNDED);
        for (OrderItem i : o.getItems()) {
            Product p = i.getProduct();
            p.setStock(p.getStock() + i.getQuantity());
            p.setSoldCount(Math.max(0, p.getSoldCount() - i.getQuantity()));
        }
        notificationService.notify(o.getStore().getOwner(),
                "Đơn #" + o.getCode() + " được khách trả hàng, hoàn tiền " + o.getTotal() + "đ", "/vendor/orders/" + o.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<OrderStatus, Long> countUserOrdersByStatus(Long userId) {
        Map<OrderStatus, Long> map = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) map.put(s, orderRepository.countByUserIdAndStatus(userId, s));
        return map;
    }

    private void cancelInternal(Order o, String reason) {
        o.setStatus(OrderStatus.CANCELLED);
        if (o.getPaymentStatus() == PaymentStatus.PAID) o.setPaymentStatus(PaymentStatus.REFUNDED);
        o.setNote((o.getNote() == null || o.getNote().isBlank() ? "" : o.getNote() + " | ") + "Lý do hủy: " + reason);
        for (OrderItem i : o.getItems()) {
            Product p = i.getProduct();
            p.setStock(p.getStock() + i.getQuantity());
        }
    }

    // ============================ Vendor ============================

    @Override
    @Transactional(readOnly = true)
    public Page<Order> storeOrders(Long storeId, OrderStatus status, String keyword, int page) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return orderRepository.searchByStore(storeId, status, kw, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    @Override
    @Transactional(readOnly = true)
    public Order getStoreOrder(Long storeId, Long orderId) {
        return orderRepository.findByIdAndStoreId(orderId, storeId).orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
    }

    @Override
    public void updateByVendor(Long storeId, Long orderId, OrderStatus newStatus) {
        Order o = getStoreOrder(storeId, orderId);
        OrderStatus cur = o.getStatus();
        if (newStatus == OrderStatus.CONFIRMED && cur == OrderStatus.NEW) {
            if (o.getPaymentMethod() == PaymentMethod.VNPAY && o.getPaymentStatus() != PaymentStatus.PAID) {
                throw new BusinessException("Đơn VNPay chưa được thanh toán");
            }
            o.setStatus(OrderStatus.CONFIRMED);
            notificationService.notify(o.getUser(), "Đơn hàng #" + o.getCode() + " đã được cửa hàng xác nhận", "/user/orders/" + o.getId());
        } else if (newStatus == OrderStatus.CANCELLED && (cur == OrderStatus.NEW || cur == OrderStatus.CONFIRMED)) {
            cancelInternal(o, "Cửa hàng hủy đơn");
            notificationService.notify(o.getUser(), "Đơn hàng #" + o.getCode() + " đã bị cửa hàng hủy", "/user/orders/" + o.getId());
        } else {
            throw new BusinessException("Không thể chuyển đơn từ \"" + cur.getLabel() + "\" sang \"" + newStatus.getLabel() + "\"");
        }
    }

    @Override
    public void assignShipper(Long storeId, Long orderId, Long shipperId) {
        Order o = getStoreOrder(storeId, orderId);
        if (o.getStatus() != OrderStatus.CONFIRMED) throw new BusinessException("Chỉ giao shipper cho đơn đã xác nhận");
        User shipper = userRepository.findById(shipperId)
                .filter(u -> u.getRole() == Role.SHIPPER && !u.isLocked())
                .orElseThrow(() -> new BusinessException("Shipper không hợp lệ"));
        o.setShipper(shipper);
        notificationService.notify(shipper, "Bạn được phân công giao đơn #" + o.getCode(), "/shipper/orders/" + o.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<OrderStatus, Long> countStoreOrdersByStatus(Long storeId) {
        Map<OrderStatus, Long> map = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) map.put(s, orderRepository.countByStoreIdAndStatus(storeId, s));
        return map;
    }

    // ============================ Shipper ============================

    @Override
    @Transactional(readOnly = true)
    public Page<Order> shipperOrders(Long shipperId, OrderStatus status, int page) {
        return orderRepository.findByShipper(shipperId, status, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    @Override
    @Transactional(readOnly = true)
    public Order getShipperOrder(Long shipperId, Long orderId) {
        return orderRepository.findByIdAndShipperId(orderId, shipperId).orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
    }

    @Override
    public void updateByShipper(Long shipperId, Long orderId, OrderStatus newStatus) {
        Order o = getShipperOrder(shipperId, orderId);
        OrderStatus cur = o.getStatus();
        boolean valid = (cur == OrderStatus.CONFIRMED && newStatus == OrderStatus.PICKED_UP)
                || (cur == OrderStatus.PICKED_UP && newStatus == OrderStatus.SHIPPING)
                || (cur == OrderStatus.SHIPPING && newStatus == OrderStatus.DELIVERED)
                || (cur == OrderStatus.SHIPPING && newStatus == OrderStatus.CANCELLED);
        if (!valid) {
            throw new BusinessException("Không thể chuyển đơn từ \"" + cur.getLabel() + "\" sang \"" + newStatus.getLabel() + "\"");
        }
        if (newStatus == OrderStatus.CANCELLED) {
            cancelInternal(o, "Giao hàng không thành công");
        } else {
            o.setStatus(newStatus);
        }
        if (newStatus == OrderStatus.DELIVERED) {
            o.setDeliveredAt(LocalDateTime.now());
            o.setPaymentStatus(PaymentStatus.PAID);
            for (OrderItem i : o.getItems()) {
                i.getProduct().setSoldCount(i.getProduct().getSoldCount() + i.getQuantity());
            }
        }
        String msg = "Đơn hàng #" + o.getCode() + ": " + o.getStatus().getLabel();
        notificationService.notify(o.getUser(), msg, "/user/orders/" + o.getId());
        notificationService.notify(o.getStore().getOwner(), msg, "/vendor/orders/" + o.getId());
    }

    // ============================ Admin ============================

    @Override
    @Transactional(readOnly = true)
    public Page<Order> adminSearch(OrderStatus status, Long storeId, String keyword, int page) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return orderRepository.adminSearch(status, storeId, kw, PageRequest.of(Math.max(page, 0), 15));
    }

    @Override
    @Transactional(readOnly = true)
    public Order getById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
    }
}
