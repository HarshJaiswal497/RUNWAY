package com.runway.engagement.client.product.dto;

import lombok.Data;

@Data
public class ProductSummaryResponse {
    private Long id;
    private String title;
    private boolean active;
}