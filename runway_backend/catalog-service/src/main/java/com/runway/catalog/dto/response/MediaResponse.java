package com.runway.catalog.dto.response;

public record MediaResponse(
        Long id,
        String fullUrl, // The base URL + relative path
        boolean isPrimary,
        Integer sortOrder
) {}
