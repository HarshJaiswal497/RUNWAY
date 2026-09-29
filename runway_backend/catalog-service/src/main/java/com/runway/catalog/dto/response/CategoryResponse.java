package com.runway.catalog.dto.response;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        long parentId
) {}