package com.runway.catalog.controller;

import com.runway.catalog.dto.response.ProductResponse;
import com.runway.catalog.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Wishlist management")
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{productId}")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<Void> add(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        wishlistService.addToWishlist(userId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<Void> remove(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get my wishlist")
    public ResponseEntity<List<ProductResponse>> getWishlist(
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(wishlistService.getWishlist(userId));
    }

    @GetMapping("/{productId}/check")
    @Operation(summary = "Check if product is wishlisted")
    public ResponseEntity<Boolean> check(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(wishlistService.isWishlisted(userId, productId));
    }
}