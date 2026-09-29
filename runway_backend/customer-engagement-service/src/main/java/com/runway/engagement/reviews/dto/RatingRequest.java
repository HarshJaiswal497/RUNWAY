package com.runway.engagement.reviews.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RatingRequest {
//    @NotNull(message = "Product Id is required")
//    private Long productId;
//
//    @NotNull(message = "User Id is required")
//    private Long userId;

    @Min(value = 1, message = "Rating should be atleast 1")
    @Max(value = 5, message = "Rating should be atmost 5")
    private  Integer rating;
}
