package com.runway.engagement.client.product;

import com.runway.engagement.client.product.dto.ProductSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "CATALOG-SERVICE",
        url = "http://localhost:8765",
        fallback = ProductClient.ProductClientFallback.class
)
public interface ProductClient {

    @GetMapping("/catalog/products/{productId}")
    ProductSummaryResponse getProductById(
            @PathVariable("productId") Long productId
    );

    @Component
    class ProductClientFallback implements ProductClient {

        @Override
        public ProductSummaryResponse getProductById(Long productId) {
            ProductSummaryResponse fallback = new ProductSummaryResponse();
            fallback.setId(productId);
            fallback.setTitle("Unknown product");
            fallback.setActive(false);
            return fallback;
        }
    }
}