package com.runway.engagement.reviews.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonPropertyOrder({"id", "productId", "userId", "rating", "createdAt"})
public class RatingResponse {
    private Long id;
    private Long productId;
    private Long userId;
    private Integer rating;
    private LocalDateTime createdAt;
}

