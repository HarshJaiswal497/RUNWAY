package com.runway.engagement.reviews.service;

import com.runway.engagement.client.product.ProductClient;
import com.runway.engagement.client.product.dto.ProductSummaryResponse;
import com.runway.engagement.notifications.model.NotificationEvent;
import com.runway.engagement.notifications.service.NotificationService;
import com.runway.engagement.reviews.exception.ReviewNotFoundException;
import com.runway.engagement.reviews.model.Rating;
import com.runway.engagement.reviews.repository.RatingRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final ProductClient productClient;
    private final NotificationService notificationService;

    public RatingService(RatingRepository ratingRepository,
                         ProductClient productClient,
                         NotificationService notificationService) {
        this.ratingRepository = ratingRepository;
        this.productClient = productClient;
        this.notificationService = notificationService;
    }

    //  PUBLIC: ratings for a product — users with a REJECTED review excluded
    public List<Rating> getRatingsByProductId(Long productId) {
        return ratingRepository.findPublicByProductId(productId);
    }

    //  CUSTOMER / ADMIN: create new rating
    public Rating createRating(Long productId, Long userId, int ratingValue) {

        ProductSummaryResponse product = productClient.getProductById(productId);

        ratingRepository.findByProductIdAndUserId(productId, userId)
                .ifPresent(r -> {
                    throw new IllegalStateException(
                            "Rating already exists for this product and user"
                    );
                });

        Rating rating = new Rating();
        rating.setProductId(productId);
        rating.setUserId(userId);
        rating.setRating(ratingValue);

        Rating saved = ratingRepository.save(rating);

        notificationService.notify(
                NotificationEvent.RATING_SUBMITTED,
                userId,
                Map.of(
                        "productId", String.valueOf(productId),
                        "productName", product.getTitle(),
                        "rating", saved.getRating()
                )
        );

        return saved;
    }

    //  CUSTOMER / ADMIN: update existing rating
    public Rating updateRating(Long productId, Long userId, int newRating) {

        ProductSummaryResponse product = productClient.getProductById(productId);

        Rating rating = ratingRepository
                .findByProductIdAndUserId(productId, userId)
                .orElseThrow(() ->
                        new ReviewNotFoundException(
                                "No rating by user " + userId + " for product " + productId
                        ));

        rating.setRating(newRating);
        Rating saved = ratingRepository.save(rating);

        notificationService.notify(
                NotificationEvent.RATING_SUBMITTED,
                userId,
                Map.of(
                        "productId", String.valueOf(productId),
                        "productName", product.getTitle(),
                        "rating", saved.getRating()
                )
        );

        return saved;
    }

    //  PUBLIC: average rating for product
    public double getAverageRatingByProductId(Long productId) {
        List<Rating> ratings = getRatingsByProductId(productId);
        return ratings.isEmpty()
                ? 0.0
                : ratings.stream()
                .mapToInt(Rating::getRating)
                .average()
                .orElse(0.0);
    }

    //  CUSTOMER / ADMIN: user’s rating for a product
    public Rating getUserRatingForProduct(Long productId, Long userId) {
        return ratingRepository
                .findByProductIdAndUserId(productId, userId)
                .orElseThrow(() ->
                        new ReviewNotFoundException(
                                "No rating by user " + userId + " for product " + productId
                        ));
    }

    //  CUSTOMER / ADMIN: delete rating (owner or admin)
    public void deleteRating(Long productId, Long userId, boolean isAdmin) {

        Rating rating = ratingRepository
                .findByProductIdAndUserId(productId, userId)
                .orElseThrow(() ->
                        new ReviewNotFoundException(
                                "No rating by user " + userId + " for product " + productId
                        ));

        if (rating.getUserId() != (userId) && !isAdmin) {
            throw new AccessDeniedException("This rating is not owned by you");
        }

        ratingRepository.delete(rating);
    }

    //  CUSTOMER / ADMIN: ratings by logged-in user
    public List<Rating> getUserRatings(Long userId) {
        return ratingRepository.findByUserId(userId);
    }

    //  ADMIN ONLY: all ratings
    public List<Rating> getAllRatings() {
        return ratingRepository.findAll();
    }
}