package com.runway.order.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO that mirrors Catalog Service's ProductResponse.
 * Used to deserialize the JSON returned by CatalogServiceClient.
 *
 * Catalog ProductResponse fields:
 *   id, sku, title, description, price, currency, categoryId, sellerId, attributes, media
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String sku;
    private String title;
    private String description;
    private BigDecimal price;
    private String currency;
    private Long categoryId;
    private Long sellerId;
    private Map<String, Object> attributes;
    private List<MediaDTO> media;
}
