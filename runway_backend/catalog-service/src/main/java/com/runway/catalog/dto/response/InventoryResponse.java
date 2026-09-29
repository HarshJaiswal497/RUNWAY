package com.runway.catalog.dto.response;

public record InventoryResponse(
        Long productId,
        Integer currentQuantity,
        String status,
        String message
) {}