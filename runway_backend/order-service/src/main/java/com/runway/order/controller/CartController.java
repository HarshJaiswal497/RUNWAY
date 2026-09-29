package com.runway.order.controller;

import com.runway.order.service.CartService;
import com.runway.order.util.ApiResponse;
import com.runway.order.util.Dtos.AddCartItemRequest;
import com.runway.order.util.Dtos.CartResponse;
import com.runway.order.util.Dtos.UpdateCartItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management")
public class CartController {

    private final CartService cartService;

    @GetMapping("/my")
    @Operation(summary = "Get my cart")
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(
            @RequestHeader("X-User-Id") Long userId) {

        log.debug("GET /api/cart/my - userId={}", userId);
        CartResponse cart = cartService.getMyCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved", cart));
    }

    @PostMapping("/my/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddCartItemRequest request) {

        log.debug("POST /api/cart/my/items - userId={}, productId={}", userId, request.getProductId());
        CartResponse cart = cartService.addItem(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cart));
    }

    @PutMapping("/my/items/{productId}")
    @Operation(summary = "Update item quantity in cart")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateCartItemRequest request) {

        log.debug("PUT /api/cart/my/items/{} - userId={}", productId, userId);
        CartResponse cart = cartService.updateItemQuantity(userId, productId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", cart));
    }

    @DeleteMapping("/my/items/{productId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") Long userId) {

        log.debug("DELETE /api/cart/my/items/{} - userId={}", productId, userId);
        CartResponse cart = cartService.removeItem(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cart));
    }

    @DeleteMapping("/my")
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(
            @RequestHeader("X-User-Id") Long userId) {

        log.debug("DELETE /api/cart/my - userId={}", userId);
        CartResponse cart = cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", cart));
    }
}