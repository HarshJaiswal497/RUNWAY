package com.runway.engagement.reviews.dto;

import com.runway.engagement.reviews.model.Review;

public class ReviewMapper {
    public static Review toEntity(ReviewRequest request) {
        Review review = new Review();
        review.setTitle(request.getTitle());
        review.setBody(request.getBody());
        review.setProductId(request.getProductId());
        return review;
    }

    public static ReviewResponse toResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setTitle(review.getTitle());
        response.setBody(review.getBody());
        response.setProductId(review.getProductId());
        response.setUserId(review.getUserId());
        response.setStatus(review.getStatus().name());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }
}
