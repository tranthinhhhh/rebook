package vn.hcmute.rebook.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.hcmute.rebook.dto.ReviewForm;
import vn.hcmute.rebook.entity.OrderItem;
import vn.hcmute.rebook.entity.Product;
import vn.hcmute.rebook.entity.Review;
import vn.hcmute.rebook.entity.enums.OrderStatus;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.exception.NotFoundException;
import vn.hcmute.rebook.repository.OrderItemRepository;
import vn.hcmute.rebook.repository.ReviewRepository;
import vn.hcmute.rebook.repository.UserRepository;
import vn.hcmute.rebook.service.NotificationService;
import vn.hcmute.rebook.service.ReviewService;
import vn.hcmute.rebook.service.StorageService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<Review> productReviews(Long productId, int page) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, PageRequest.of(Math.max(page, 0), 5));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> reviewableItems(Long userId, Long productId) {
        return userId == null ? List.of() : orderItemRepository.findReviewable(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderItem getReviewableItem(Long userId, Long orderItemId) {
        OrderItem item = orderItemRepository.findByIdAndUserId(orderItemId, userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm đã mua"));
        if (item.getOrder().getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Chỉ được đánh giá sách trong đơn đã giao thành công");
        }
        if (item.isReviewed()) throw new BusinessException("Bạn đã đánh giá sách này rồi");
        return item;
    }

    @Override
    public void create(Long userId, ReviewForm form, MultipartFile media) {
        OrderItem item = getReviewableItem(userId, form.getOrderItemId());
        String content = form.getContent() == null ? "" : form.getContent().trim();
        if (content.length() < 50) throw new BusinessException("Nội dung đánh giá tối thiểu 50 ký tự");
        String mediaUrl = storageService.upload(media, "reviews");
        String mediaType = null;
        if (mediaUrl != null) {
            String ct = media.getContentType() == null ? "" : media.getContentType();
            mediaType = ct.startsWith("video") ? "video" : "image";
        }
        Product p = item.getProduct();
        reviewRepository.save(Review.builder()
                .user(userRepository.getReferenceById(userId))
                .product(p).orderItem(item)
                .rating(form.getRating()).content(content)
                .mediaUrl(mediaUrl).mediaType(mediaType)
                .build());
        item.setReviewed(true);
        // Cập nhật điểm trung bình
        double total = p.getRatingAvg() * p.getRatingCount() + form.getRating();
        p.setRatingCount(p.getRatingCount() + 1);
        p.setRatingAvg(Math.round(total / p.getRatingCount() * 10) / 10.0);
        notificationService.notify(p.getStore().getOwner(),
                "Sách \"" + p.getTitle() + "\" nhận đánh giá " + form.getRating() + " sao", "/books/" + p.getId() + "#reviews");
    }
}
