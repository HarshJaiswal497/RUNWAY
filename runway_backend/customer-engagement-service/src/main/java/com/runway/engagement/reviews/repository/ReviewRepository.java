package com.runway.engagement.reviews.repository;

import com.runway.engagement.reviews.model.Review;
import com.runway.engagement.reviews.model.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdAndStatus(Long productId, ReviewStatus status);

    List<Review> findByUserId(Long userId);

    List<Review> findByProductIdAndUserId(Long productId, Long userId);
}
