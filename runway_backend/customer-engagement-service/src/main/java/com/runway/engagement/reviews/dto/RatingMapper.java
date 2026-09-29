package com.runway.engagement.reviews.dto;

import com.runway.engagement.reviews.model.Rating;

public class RatingMapper {
    public static Rating toEntity(RatingRequest request) {
        Rating rating = new Rating();
//        rating.setProductId(request.getProductId());
//        rating.setUserId(request.getUserId());
        rating.setRating(request.getRating());
        return rating;
    }

    public static RatingResponse toResponse(Rating rating) {
        RatingResponse response = new RatingResponse();
        response.setId(rating.getId());
        response.setProductId(rating.getProductId());
        response.setUserId(rating.getUserId());
        response.setRating(rating.getRating());
        response.setCreatedAt(rating.getCreatedAt());
        return response;
    }
}
