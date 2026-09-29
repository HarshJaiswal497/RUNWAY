package com.runway.catalog.mapper;

import com.runway.catalog.dto.response.CategoryResponse;
import com.runway.catalog.dto.response.MediaResponse;
import com.runway.catalog.dto.response.ProductResponse;
import com.runway.catalog.model.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductMapper {

    // This pulls your local IP/Domain from properties to build the full image URL
    @Value("${app.image.base-url:http://localhost:8765/api/v1/catalog/images/}")
    private String imageBaseUrl;

    public ProductResponse toResponse(Product product) {
        // 1. Map Category to its DTO
        CategoryResponse categoryDto = new CategoryResponse(
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getCategory().getDescription(),
                product.getCategory().getParent() != null ? product.getCategory().getParent().getId() : 0
        );

        // 2. Map Media list to MediaResponse DTOs (Injecting the full URL)
        List<MediaResponse> mediaDtos = product.getMedia().stream()
                .map(m -> new MediaResponse(
                        m.getId(),
                        imageBaseUrl + m.getUrlPath(), // Combines base + relative path
                        m.isPrimary(),
                        m.getSortOrder()
                ))
                .collect(Collectors.toList());

        // 3. Return the final ProductResponse Record
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getCategory().getId(),
                product.getSellerId(),
                product.getAttributes(),
                mediaDtos
        );
    }
}