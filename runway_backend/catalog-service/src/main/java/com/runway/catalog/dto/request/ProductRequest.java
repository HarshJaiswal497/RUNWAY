package com.runway.catalog.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record ProductRequest(
        @NotNull(message = "SKU is required")
        String sku,
        @NotNull(message = "Title is required")
        String title,
        String description,
        @NotNull(message = "Price is required")
        @Min(value = 0, message = "Price must be non-negative")
        BigDecimal price,
        @Size(max = 3, message = "Currency code must be 3 characters")
        String currency,
        @NotNull(message = "Category ID is required")
        Long categoryId,
        Integer initialQuantity,
        Map<String, Object> attributes
) {}