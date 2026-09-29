package com.runway.engagement.reviews;

import com.runway.engagement.reviews.controller.ReviewController;
import com.runway.engagement.reviews.dto.ReviewRequest;
import com.runway.engagement.reviews.dto.ReviewStatusUpdateRequest;
import com.runway.engagement.reviews.model.Review;
import com.runway.engagement.reviews.model.ReviewStatus;
import com.runway.engagement.reviews.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController controller;

    // -----------------------------------------
    // POST /products/{productId}/reviews
    // -----------------------------------------
    @Test
    void createReview_returnsCreated() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        ReviewRequest request = new ReviewRequest();
        request.setProductId(99L);
        request.setTitle("Good product");
        request.setBody("Works well");

        Review saved = new Review();
        saved.setId(1L);
        saved.setProductId(99L);
        saved.setUserId(10L);
        saved.setStatus(ReviewStatus.PENDING);

        when(reviewService.createReview(any(Review.class), eq(10L)))
                .thenReturn(saved);

        var response = controller.createReview(99L, request, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        verify(reviewService).createReview(any(Review.class), eq(10L));
    }

    // -----------------------------------------
    // GET /products/{productId}/reviews (PUBLIC)
    // -----------------------------------------
    @Test
    void getProductReviews_public_returnsApproved() {
        Review review = new Review();
        review.setStatus(ReviewStatus.APPROVED);

        when(reviewService.getApprovedReviewsByProduct(99L))
                .thenReturn(List.of(review));

        var response = controller.getProductReviews(99L, false, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        verify(reviewService).getApprovedReviewsByProduct(99L);
    }

    // -----------------------------------------
    // GET /products/{productId}/reviews?mine=true
    // -----------------------------------------
    @Test
    void getProductReviews_mineTrue_returnsUserReviews() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        Review review = new Review();
        review.setUserId(10L);
        review.setStatus(ReviewStatus.APPROVED);

        when(reviewService.getReviewsByUserForProduct(99L, 10L))
                .thenReturn(List.of(review));

        var response = controller.getProductReviews(99L, true, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        verify(reviewService).getReviewsByUserForProduct(99L, 10L);
    }

    // -----------------------------------------
    // GET /reviews?mine=true
    // -----------------------------------------
    @Test
    void getReviews_mineTrue_returnsUserReviews() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        Review review = new Review();
        review.setStatus(ReviewStatus.APPROVED);

        when(reviewService.getReviewsByUser(10L))
                .thenReturn(List.of(review));

        var response = controller.getReviews(true, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        verify(reviewService).getReviewsByUser(10L);
    }

    // -----------------------------------------
    // GET /reviews (ADMIN)
    // -----------------------------------------
    @Test
    void getReviews_admin_returnsAll() {
        Authentication auth = mock(Authentication.class);

        Review review1 = new Review();
        review1.setStatus(ReviewStatus.APPROVED);

        Review review2 = new Review();
        review2.setStatus(ReviewStatus.PENDING);

        when(reviewService.getAllReviews())
                .thenReturn(List.of(review1, review2));

        var response = controller.getReviews(false, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);

        verify(reviewService).getAllReviews();
    }

    // -----------------------------------------
    // GET /reviews/{id}
    // -----------------------------------------
    @Test
    void getReviewById_returnsReview() {
        Review review = new Review();
        review.setId(5L);
        review.setStatus(ReviewStatus.APPROVED);

        when(reviewService.getReviewById(5L))
                .thenReturn(review);

        var response = controller.getReviewById(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        verify(reviewService).getReviewById(5L);
    }

    // -----------------------------------------
    // PUT /reviews/{id}/status (ADMIN)
    // -----------------------------------------
    @Test
    void updateReviewStatus_returnsUpdated() {
        ReviewStatusUpdateRequest request = new ReviewStatusUpdateRequest();
        request.setStatus(ReviewStatus.APPROVED);

        Review review = new Review();
        review.setStatus(ReviewStatus.APPROVED);

        when(reviewService.updateReviewStatus(1L, ReviewStatus.APPROVED))
                .thenReturn(review);

        var response = controller.updateReviewStatus(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(reviewService)
                .updateReviewStatus(1L, ReviewStatus.APPROVED);
    }

    // -----------------------------------------
    // PUT /reviews/{id} (ADMIN)
    // -----------------------------------------
    @Test
    void updateReview_adminFlagTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        Collection<GrantedAuthority> authorities =
                List.of((GrantedAuthority) () -> "ROLE_ADMIN");
        doReturn(authorities).when(auth).getAuthorities();

        Review updated = new Review();

        updated.setStatus(ReviewStatus.APPROVED);

        when(reviewService.updateReview(
                eq(1L), any(Review.class), eq(10L), eq(true)))
                .thenReturn(updated);

        ReviewRequest request = new ReviewRequest();
        request.setProductId(99L);
        request.setTitle("Updated");

        var response = controller.updateReview(1L, request, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(reviewService)
                .updateReview(eq(1L), any(), eq(10L), eq(true));
    }

    // -----------------------------------------
    // DELETE /reviews/{id}
    // -----------------------------------------
    @Test
    void deleteReview_admin() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        Collection<GrantedAuthority> authorities =
                List.of((GrantedAuthority) () -> "ROLE_ADMIN");
        doReturn(authorities).when(auth).getAuthorities();

        var response = controller.deleteReview(1L, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(reviewService).deleteReview(1L, 10L, true);
    }
}