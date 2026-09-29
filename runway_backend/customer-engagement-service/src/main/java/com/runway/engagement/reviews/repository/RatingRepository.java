package com.runway.engagement.reviews.repository;

import com.runway.engagement.reviews.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByProductId(Long productId);
    Optional<Rating> findByProductIdAndUserId(Long productId, Long userId);
    List<Rating> findByUserId(Long userId);

    /**
     * Returns ratings for a product, excluding any user whose review
     * for this product has been REJECTED. Users who only rated (no review),
     * or whose review is PENDING/APPROVED, are still included.
     */
    @Query("""
            SELECT r FROM Rating r
            WHERE r.productId = :productId
              AND r.userId NOT IN (
                SELECT rev.userId FROM Review rev
                WHERE rev.productId = :productId
                  AND rev.status = com.runway.engagement.reviews.model.ReviewStatus.REJECTED
              )
            """)
    List<Rating> findPublicByProductId(@Param("productId") Long productId);
}
