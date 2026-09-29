package com.runway.engagement.reviews;

import com.runway.engagement.client.product.ProductClient;
import com.runway.engagement.client.product.dto.ProductSummaryResponse;
import com.runway.engagement.notifications.model.NotificationEvent;
import com.runway.engagement.notifications.service.NotificationService;
import com.runway.engagement.reviews.exception.ReviewNotFoundException;
import com.runway.engagement.reviews.model.Rating;
import com.runway.engagement.reviews.repository.RatingRepository;
import com.runway.engagement.reviews.service.RatingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RatingService service;

    // -----------------------------------------
    // getRatingsByProductId
    // -----------------------------------------
    @Test
    void getRatingsByProductId_returnsRatings() {
        Rating rating = new Rating();
        rating.setProductId(99L);

        when(ratingRepository.findPublicByProductId(99L))
                .thenReturn(List.of(rating));

        List<Rating> result = service.getRatingsByProductId(99L);

        assertThat(result).hasSize(1);
        verify(ratingRepository).findPublicByProductId(99L);
    }

    // -----------------------------------------
    // createRating – success
    // -----------------------------------------
    @Test
    void createRating_createsNewRating_andSendsNotification() {
        ProductSummaryResponse product = new ProductSummaryResponse();
        product.setTitle("iPhone");

        when(productClient.getProductById(99L))
                .thenReturn(product);
        when(ratingRepository.findByProductIdAndUserId(99L, 10L))
                .thenReturn(Optional.empty());

        Rating saved = new Rating();
        saved.setProductId(99L);
        saved.setUserId(10L);
        saved.setRating(5);

        when(ratingRepository.save(any(Rating.class)))
                .thenReturn(saved);

        Rating result = service.createRating(99L, 10L, 5);

        assertThat(result.getRating()).isEqualTo(5);

        verify(notificationService).notify(
                NotificationEvent.RATING_SUBMITTED,
                10L,
                Map.of(
                        "productId", "99",
                        "productName", "iPhone",
                        "rating", 5
                )
        );
    }

    // -----------------------------------------
    // createRating – already exists
    // -----------------------------------------
    @Test
    void createRating_whenAlreadyExists_throwsException() {
        Rating existing = new Rating();

        when(ratingRepository.findByProductIdAndUserId(99L, 10L))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                service.createRating(99L, 10L, 5)
        ).isInstanceOf(IllegalStateException.class);

        verify(ratingRepository, never()).save(any());
    }

    // -----------------------------------------
    // updateRating – success
    // -----------------------------------------
    @Test
    void updateRating_updatesRating_andSendsNotification() {
        ProductSummaryResponse product = new ProductSummaryResponse();
        product.setTitle("Pixel");

        Rating existing = new Rating();
        existing.setProductId(99L);
        existing.setUserId(10L);
        existing.setRating(3);

        when(productClient.getProductById(99L))
                .thenReturn(product);
        when(ratingRepository.findByProductIdAndUserId(99L, 10L))
                .thenReturn(Optional.of(existing));
        when(ratingRepository.save(existing))
                .thenReturn(existing);

        Rating result = service.updateRating(99L, 10L, 4);

        assertThat(result.getRating()).isEqualTo(4);

        verify(notificationService).notify(
                NotificationEvent.RATING_SUBMITTED,
                10L,
                Map.of(
                        "productId", "99",
                        "productName", "Pixel",
                        "rating", 4
                )
        );
    }

    // -----------------------------------------
    // updateRating – not found
    // -----------------------------------------
    @Test
    void updateRating_notFound_throwsException() {
        when(ratingRepository.findByProductIdAndUserId(99L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateRating(99L, 10L, 4)
        ).isInstanceOf(ReviewNotFoundException.class);
    }

    // -----------------------------------------
    // getAverageRatingByProductId – empty
    // -----------------------------------------
    @Test
    void getAverageRatingByProductId_noRatings_returnsZero() {
        when(ratingRepository.findPublicByProductId(99L))
                .thenReturn(List.of());

        double avg = service.getAverageRatingByProductId(99L);

        assertThat(avg).isZero();
    }

    // -----------------------------------------
    // getAverageRatingByProductId – non empty
    // -----------------------------------------
    @Test
    void getAverageRatingByProductId_calculatesAverage() {
        Rating r1 = new Rating(); r1.setRating(4);
        Rating r2 = new Rating(); r2.setRating(5);

        when(ratingRepository.findPublicByProductId(99L))
                .thenReturn(List.of(r1, r2));

        double avg = service.getAverageRatingByProductId(99L);

        assertThat(avg).isEqualTo(4.5);
    }

    // -----------------------------------------
    // getUserRatingForProduct – success
    // -----------------------------------------
    @Test
    void getUserRatingForProduct_returnsRating() {
        Rating rating = new Rating();

        when(ratingRepository.findByProductIdAndUserId(99L, 10L))
                .thenReturn(Optional.of(rating));

        Rating result =
                service.getUserRatingForProduct(99L, 10L);

        assertThat(result).isNotNull();
    }

    // -----------------------------------------
    // getUserRatingForProduct – not found
    // -----------------------------------------
    @Test
    void getUserRatingForProduct_notFound_throwsException() {
        when(ratingRepository.findByProductIdAndUserId(99L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getUserRatingForProduct(99L, 10L)
        ).isInstanceOf(ReviewNotFoundException.class);
    }

    // -----------------------------------------
    // deleteRating – owner
    // -----------------------------------------
    @Test
    void deleteRating_ownerDeletesSuccessfully() {
        Rating rating = new Rating();
        rating.setUserId(10L);

        when(ratingRepository.findByProductIdAndUserId(99L, 10L))
                .thenReturn(Optional.of(rating));

        service.deleteRating(99L, 10L, false);

        verify(ratingRepository).delete(rating);
    }

    // -----------------------------------------
    // deleteRating – admin override
    // -----------------------------------------
    @Test
    void deleteRating_adminDeletesSuccessfully() {
        Rating rating = new Rating();
        rating.setUserId(20L);

        when(ratingRepository.findByProductIdAndUserId(99L, 10L))
                .thenReturn(Optional.of(rating));

        service.deleteRating(99L, 10L, true);

        verify(ratingRepository).delete(rating);
    }

    // -----------------------------------------
    // deleteRating – access denied
    // -----------------------------------------
    @Test
    void deleteRating_notOwnerAndNotAdmin_throwsAccessDenied() {
        Rating rating = new Rating();
        rating.setUserId(20L);

        when(ratingRepository.findByProductIdAndUserId(99L, 10L))
                .thenReturn(Optional.of(rating));

        assertThatThrownBy(() ->
                service.deleteRating(99L, 10L, false)
        ).isInstanceOf(AccessDeniedException.class);
    }

    // -----------------------------------------
    // getUserRatings
    // -----------------------------------------
    @Test
    void getUserRatings_returnsRatings() {
        when(ratingRepository.findByUserId(10L))
                .thenReturn(List.of(new Rating()));

        List<Rating> result = service.getUserRatings(10L);

        assertThat(result).hasSize(1);
        verify(ratingRepository).findByUserId(10L);
    }

    // -----------------------------------------
    // getAllRatings
    // -----------------------------------------
    @Test
    void getAllRatings_returnsAll() {
        when(ratingRepository.findAll())
                .thenReturn(List.of(new Rating(), new Rating()));

        List<Rating> result = service.getAllRatings();

        assertThat(result).hasSize(2);
        verify(ratingRepository).findAll();
    }
}