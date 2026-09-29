package com.runway.catalog.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductResponse(
        Long id,
        String sku,
        String title,
        String description,
        BigDecimal price,
        String currency,
        Long categoryId,
        Long sellerId,
        Map<String, Object> attributes,
        List<MediaResponse> media // Nested DTO for image details
) {}