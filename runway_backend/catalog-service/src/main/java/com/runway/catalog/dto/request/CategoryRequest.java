package com.runway.catalog.dto.request;

import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotNull(message = "Name is required")
        String name,
        String description,
        Long parentId // To support hierarchy
) {}
