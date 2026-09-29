package com.runway.order.client;

import com.runway.order.dto.catalog.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign Client for CATALOG-SERVICE (registered in Eureka as "CATALOG-SERVICE").
 *
 * Maps to:
 *   ProductController   → GET   /catalog/products/{id}              → returns ProductResponse
 *   InventoryController → GET   /catalog/inventory/{id}             → returns Integer (stock)
 *   InventoryController → PATCH /catalog/inventory/{id}/update      → deduct/add stock
 */
@FeignClient(name = "CATALOG-SERVICE")
public interface CatalogServiceClient {

    /**
     * Get product details by ID.
     * Catalog endpoint: GET /catalog/products/{id}
     */
    @GetMapping("/catalog/products/{id}")
    ProductDTO getProductById(@PathVariable("id") Long productId);

    /**
     * Get current stock quantity for a product.
     * Catalog endpoint: GET /catalog/inventory/{productId}
     */
    @GetMapping("/catalog/inventory/{productId}")
    Integer getStock(@PathVariable("productId") Long productId);

    /**
     * Deduct or add stock for a product.
     * Catalog endpoint: PATCH /catalog/inventory/{productId}/update?amount=-X
     *
     * Pass negative amount to deduct (e.g., -2 means remove 2 units).
     * Pass positive amount to restock (e.g., +5 means add 5 units).
     *
     * Requires X-User-Id and X-USER-ROLE headers (ADMIN role bypasses ownership check).
     */
    @PatchMapping("/catalog/inventory/{productId}/update")
    void deductStock(@PathVariable("productId") Long productId,
                     @RequestParam("amount") Integer amount,
                     @RequestHeader("X-User-Id") Long userId,
                     @RequestHeader("X-User-Role") String role);

}
