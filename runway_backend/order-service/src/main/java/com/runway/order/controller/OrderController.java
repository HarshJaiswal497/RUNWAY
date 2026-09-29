package com.runway.order.controller;

import com.runway.order.service.OrderService;
import com.runway.order.util.ApiResponse;
import com.runway.order.util.Dtos.CheckoutRequest;
import com.runway.order.util.Dtos.OrderResponse;
import com.runway.order.util.Dtos.UpdateOrderStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement and management")
public class OrderController {

    private final OrderService orderService;

    // ─────────────────────────────────────────────
    // STATIC ROUTES FIRST (before any /{variable})
    // ─────────────────────────────────────────────

    @GetMapping("/exists")
    @Operation(summary = "Check if user purchased a product (internal)")
    public ResponseEntity<Boolean> hasUserPurchasedProduct(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "DELIVERED") String status) {

        log.debug("GET /api/orders/exists - userId={} productId={} status={}", userId, productId, status);
        boolean purchased = orderService.hasUserPurchasedProduct(userId, productId, status);
        return ResponseEntity.ok(purchased);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my orders (paginated)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.debug("GET /api/orders/my - userId={}", userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> orders = orderService.getMyOrders(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", orders));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Place order (checkout cart)")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody CheckoutRequest request) {

        log.debug("POST /api/orders/checkout - userId={}", userId);
        OrderResponse order = orderService.placeOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed! Order #" + order.getOrderNumber(), order));
    }

    @GetMapping("/admin/all")
    @Operation(summary = "Get ALL orders - Admin")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        authorizeAdmin(role);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success("All orders retrieved", orders));
    }

    @PutMapping("/admin/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        authorizeAdmin(role);
        log.debug("PUT /api/orders/admin/{}/status - status={} by userId={}",
                id, request.getStatus(), userId);

        OrderResponse order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated to " + order.getStatus(), order));
    }

    // ─────────────────────────────────────────────
    // DYNAMIC ROUTES LAST (/{variable} patterns)
    // ─────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.debug("GET /api/orders/{}", id);
        OrderResponse order = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Order found", order));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel order (PENDING_PAYMENT only)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        log.debug("PUT /api/orders/{}/cancel", id);
        OrderResponse order = orderService.cancelOrder(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", order));
    }
    @GetMapping("/{id}/internal")
    @Operation(summary = "Internal - Get order for service-to-service calls")
    public ResponseEntity<OrderResponse> getOrderInternal(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        log.debug("GET /api/orders/{}/internal - service call userId={}", id, userId);
        OrderResponse order = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(order);
    }
    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private void authorizeAdmin(String role) {
        if (role == null || !role.equalsIgnoreCase("ADMIN")) {
            throw new AccessDeniedException("Admin role required");
        }
    }
}