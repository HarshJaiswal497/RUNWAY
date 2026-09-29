package com.runway.engagement.reviews.service;

import com.runway.engagement.client.order.OrderClient;
import com.runway.engagement.client.product.ProductClient;
import com.runway.engagement.reviews.exception.ReviewNotFoundException;
import com.runway.engagement.reviews.model.Review;
import com.runway.engagement.reviews.model.ReviewStatus;
import com.runway.engagement.reviews.repository.ReviewRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.runway.engagement.notifications.service.NotificationService;
import com.runway.engagement.notifications.model.NotificationEvent;
import java.util.Map;


import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderClient orderClient;
    private final ProductClient productClient;
    private final NotificationService notificationService;

    public ReviewService(ReviewRepository reviewRepository,
                         OrderClient orderClient,
                         ProductClient productClient,
                         NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.orderClient = orderClient;
        this.productClient = productClient;
        this.notificationService = notificationService;
    }

    //  PUBLIC: approved reviews for product
    public List<Review> getApprovedReviewsByProduct(Long productId) {
        return reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.APPROVED);
    }

    //  CUSTOMER: my reviews
    public List<Review> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserId(userId);
    }

    //  CUSTOMER: my reviews for product
    public List<Review> getReviewsByUserForProduct(Long productId, Long userId) {
        return reviewRepository.findByProductIdAndUserId(productId, userId);
    }

    //  CUSTOMER / ADMIN: create review
    public Review createReview(Review review, Long userId) {

        Boolean hasPurchased = orderClient.hasUserPurchasedProduct(
                userId,
                review.getProductId(),
                "DELIVERED"
        );

        if (!Boolean.TRUE.equals(hasPurchased)) {
            throw new AccessDeniedException(
                    "User must purchase and receive product before reviewing"
            );
        }

        productClient.getProductById(review.getProductId());


        List<Review> existing =
                reviewRepository.findByProductIdAndUserId(
                        review.getProductId(),
                        userId
                );

        if (!existing.isEmpty()) {
            throw new IllegalStateException(
                    "Review already exists for this product and user"
            );
        }


        review.setUserId(userId);
        review.setStatus(ReviewStatus.PENDING);


        Review saved = reviewRepository.save(review);

        notificationService.notify(
                NotificationEvent.REVIEW_SUBMITTED,
                userId,
                Map.of(
                        "productId", String.valueOf(review.getProductId()),
                        "productName", productClient.getProductById(review.getProductId()).getTitle()
                )
        );

        return saved;

    }

    //  ADMIN: all reviews (all statuses)
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    //  ADMIN: get review by id
    public Review getReviewById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() ->
                        new ReviewNotFoundException("Review not found: " + id)
                );
    }

    //  ADMIN: approve / reject review
    public Review updateReviewStatus(Long id, ReviewStatus newStatus) {

        Review review = getReviewById(id);

        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING reviews can be approved or rejected"
            );
        }

        review.setStatus(newStatus);

        Review updated = reviewRepository.save(review);

        NotificationEvent event =
                newStatus == ReviewStatus.APPROVED
                        ? NotificationEvent.REVIEW_APPROVED
                        : NotificationEvent.REVIEW_REJECTED;

        notificationService.notify(
                event,
                review.getUserId(),
                Map.of(
                        "productId", String.valueOf(review.getProductId()),
                        "productName", productClient.getProductById(review.getProductId()).getTitle()
                )
        );

        return updated;

    }

    //  CUSTOMER (owner) or ADMIN: update content
    public Review updateReview(Long id,
                               Review updatedReview,
                               Long userId,
                               boolean isAdmin) {

        Review existing = getReviewById(id);

        if (existing.getUserId()!= (userId) && !isAdmin) {
            throw new AccessDeniedException("Review not owned by user");
        }

        existing.setTitle(updatedReview.getTitle());
        existing.setBody(updatedReview.getBody());
        existing.setRating(updatedReview.getRating());

        if (!isAdmin) {
            existing.setStatus(ReviewStatus.PENDING);
        }

        return reviewRepository.save(existing);
    }

    //  CUSTOMER (owner) or ADMIN: delete review
    public void deleteReview(Long id, Long userId, boolean isAdmin) {

        Review review = getReviewById(id);

        if (review.getUserId() != (userId) && !isAdmin) {
            throw new AccessDeniedException("Review not owned by user");
        }

        reviewRepository.delete(review);
    }
}