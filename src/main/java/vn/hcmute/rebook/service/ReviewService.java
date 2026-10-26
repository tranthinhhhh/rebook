package vn.hcmute.rebook.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import vn.hcmute.rebook.dto.ReviewForm;
import vn.hcmute.rebook.entity.OrderItem;
import vn.hcmute.rebook.entity.Review;

import java.util.List;

public interface ReviewService {
    Page<Review> productReviews(Long productId, int page);

    List<OrderItem> reviewableItems(Long userId, Long productId);

    OrderItem getReviewableItem(Long userId, Long orderItemId);

    void create(Long userId, ReviewForm form, MultipartFile media);
}
