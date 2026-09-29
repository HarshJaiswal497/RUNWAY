package com.runway.engagement.reviews.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonPropertyOrder({"id", "productId", "userId", "title", "body", "status", "createdAt"})
public class ReviewResponse {
    private Long id;
    private String title;
    private String body;
    private String status;
    private Long productId;
    private Long userId;
    private LocalDateTime createdAt;
}
