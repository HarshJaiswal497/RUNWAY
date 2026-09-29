package com.runway.catalog.controller;

import com.runway.catalog.dto.request.ProductRequest;
import com.runway.catalog.dto.response.ProductResponse;
import com.runway.catalog.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;

import java.util.List;

@RestController
@RequestMapping("/catalog/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management endpoints")
public class ProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart("product") String productJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestHeader("X-User-Id") Long sellerId
    ) throws JsonProcessingException {

        // Manually convert the JSON string to your Record/DTO
        // This works even if the client sends it as 'text/plain' or no type
        ProductRequest request = objectMapper.readValue(productJson, ProductRequest.class);

        // 3. MANUAL VALIDATION
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            // Throwing this will trigger your GlobalExceptionHandler
            throw new ConstraintViolationException(violations);
        }

        ProductResponse response = productService.createProduct(request, images, sellerId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategoryId(categoryId));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ProductResponse>> getSellerProducts(@PathVariable Long sellerId) {
        return ResponseEntity.ok(productService.getProductsBySeller(sellerId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            @RequestHeader(name = "X-User-Id", required = false) Long userId,
            @RequestHeader(name = "X-User-Role", required = false) String role) {

        // Fallback: check uppercase headers if lowercase not provided
        if (userId == null) {
            throw new IllegalArgumentException("X-User-Id header is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("X-User-Role header is required");
        }

        return ResponseEntity.ok(productService.updateProduct(id, request, userId, role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("X-USER-ID") Long userId,
            @RequestHeader("X-USER-ROLE") String role) {

        productService.deleteProduct(id, userId, role);
        return ResponseEntity.noContent().build();
    }
}