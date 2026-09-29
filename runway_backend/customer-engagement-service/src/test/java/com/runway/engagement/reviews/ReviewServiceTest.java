package com.runway.engagement.reviews;

import com.runway.engagement.client.order.OrderClient;
import com.runway.engagement.client.product.ProductClient;
import com.runway.engagement.reviews.exception.ReviewNotFoundException;
import com.runway.engagement.reviews.model.Review;
import com.runway.engagement.reviews.model.ReviewStatus;
import com.runway.engagement.reviews.repository.ReviewRepository;
import com.runway.engagement.reviews.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import com.runway.engagement.client.product.dto.ProductSummaryResponse;
import com.runway.engagement.notifications.service.NotificationService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderClient orderClient;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private ReviewService service;

    // ------------------------------------------------
    // getApprovedReviewsByProduct
    // ------------------------------------------------
    @Test
    void getApprovedReviewsByProduct_returnsApprovedOnly() {
        Review review = new Review();
        review.setStatus(ReviewStatus.APPROVED);

        when(reviewRepository.findByProductIdAndStatus(99L, ReviewStatus.APPROVED))
                .thenReturn(List.of(review));

        List<Review> result = service.getApprovedReviewsByProduct(99L);

        assertThat(result).hasSize(1);
        verify(reviewRepository)
                .findByProductIdAndStatus(99L, ReviewStatus.APPROVED);
    }

    // ------------------------------------------------
    // getReviewsByUser
    // ------------------------------------------------
    @Test
    void getReviewsByUser_returnsReviews() {
        when(reviewRepository.findByUserId(10L))
                .thenReturn(List.of(new Review()));

        assertThat(service.getReviewsByUser(10L)).hasSize(1);
    }

    // ------------------------------------------------
    // getReviewsByUserForProduct
    // ------------------------------------------------
    @Test
    void getReviewsByUserForProduct_returnsReviews() {
        when(reviewRepository.findByProductIdAndUserId(99L, 10L))
                .thenReturn(List.of(new Review()));

        assertThat(service.getReviewsByUserForProduct(99L, 10L)).hasSize(1);
    }

    // ------------------------------------------------
    // createReview - success
    // ------------------------------------------------
    @Test
    void createReview_success_setsPendingAndUser() {
        Review review = new Review();
        review.setProductId(99L);

        when(orderClient.hasUserPurchasedProduct(10L, 99L, "DELIVERED"))
                .thenReturn(true);

        ProductSummaryResponse product = new ProductSummaryResponse();
        product.setTitle("iPhone");

        when(productClient.getProductById(99L))
                .thenReturn(product);

        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Review result = service.createReview(review, 10L);

        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(ReviewStatus.PENDING);
    }

    // ------------------------------------------------
    // createReview - not purchased
    // ------------------------------------------------
    @Test
    void createReview_notPurchased_throwsAccessDenied() {
        Review review = new Review();
        review.setProductId(99L);

        when(orderClient.hasUserPurchasedProduct(10L, 99L, "DELIVERED"))
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.createReview(review, 10L)
        ).isInstanceOf(AccessDeniedException.class);
    }

    // ------------------------------------------------
    // getAllReviews
    // ------------------------------------------------
    @Test
    void getAllReviews_returnsAll() {
        when(reviewRepository.findAll())
                .thenReturn(List.of(new Review()));

        assertThat(service.getAllReviews()).hasSize(1);
    }

    // ------------------------------------------------
    // getReviewById - success
    // ------------------------------------------------
    @Test
    void getReviewById_whenFound_returnsReview() {
        Review review = new Review();
        review.setId(1L);

        when(reviewRepository.findById(1L))
                .thenReturn(Optional.of(review));

        Review result = service.getReviewById(1L);

        assertThat(result).isSameAs(review);
        verify(reviewRepository).findById(1L);
    }

    // ------------------------------------------------
    // getReviewById - not found
    // ------------------------------------------------
    @Test
    void getReviewById_whenNotFound_throwsException() {
        when(reviewRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getReviewById(99L)
        ).isInstanceOf(ReviewNotFoundException.class)
                .hasMessage("Review not found: 99");
    }

    // ------------------------------------------------
    // updateReviewStatus
    // ------------------------------------------------
    @Test
    void updateReviewStatus_pending_only() {
        Review review = new Review();
        review.setStatus(ReviewStatus.PENDING);
        review.setProductId(99L);
        review.setUserId(10L);

        ProductSummaryResponse product = new ProductSummaryResponse();
        product.setTitle("iPhone");

        when(productClient.getProductById(99L))
                .thenReturn(product);

        when(reviewRepository.findById(1L))
                .thenReturn(Optional.of(review));
        when(reviewRepository.save(review))
                .thenReturn(review);

        Review result =
                service.updateReviewStatus(1L, ReviewStatus.APPROVED);

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    void updateReviewStatus_nonPending_throwsIllegalState() {
        Review review = new Review();
        review.setStatus(ReviewStatus.APPROVED);

        when(reviewRepository.findById(1L))
                .thenReturn(Optional.of(review));

        assertThatThrownBy(() ->
                service.updateReviewStatus(1L, ReviewStatus.REJECTED)
        ).isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------
    // updateReview
    // ------------------------------------------------
    @Test
    void updateReview_owner_allowed() {
        Review existing = new Review();
        existing.setUserId(10L);
        existing.setStatus(ReviewStatus.APPROVED);

        Review updated = new Review();
        updated.setTitle("t");
        updated.setBody("b");
        updated.setRating(4);

        when(reviewRepository.findById(1L))
                .thenReturn(Optional.of(existing));
        when(reviewRepository.save(existing))
                .thenReturn(existing);

        Review result =
                service.updateReview(1L, updated, 10L, false);

        assertThat(result.getStatus()).isEqualTo(ReviewStatus.PENDING);
    }

    @Test
    void updateReview_notOwner_notAdmin_denied() {
        Review existing = new Review();
        existing.setUserId(20L);

        when(reviewRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                service.updateReview(1L, new Review(), 10L, false)
        ).isInstanceOf(AccessDeniedException.class);
    }

    // ------------------------------------------------
    // deleteReview
    // ------------------------------------------------
    @Test
    void deleteReview_owner_allowed() {
        Review review = new Review();
        review.setUserId(10L);

        when(reviewRepository.findById(1L))
                .thenReturn(Optional.of(review));

        service.deleteReview(1L, 10L, false);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_notOwner_notAdmin_denied() {
        Review review = new Review();
        review.setUserId(20L);

        when(reviewRepository.findById(1L))
                .thenReturn(Optional.of(review));

        assertThatThrownBy(() ->
                service.deleteReview(1L, 10L, false)
        ).isInstanceOf(AccessDeniedException.class);
    }
}