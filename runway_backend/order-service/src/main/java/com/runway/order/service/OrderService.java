package com.runway.order.service;

import com.runway.order.model.Order;
import com.runway.order.util.Dtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * OrderService — Interface
 * Called by OrderController. Never changes.
 */
public interface OrderService {

    /** Checkout cart → create order */
    OrderResponse placeOrder(Long userId, CheckoutRequest request);

    /** Get specific order — owner or admin only */
    OrderResponse getOrderById(Long orderId, Long userId);

    /** Get all orders for current user */
    Page<OrderResponse> getMyOrders(Long userId, Pageable pageable);

    /** Get ALL orders — Admin only */
    Page<OrderResponse> getAllOrders(Pageable pageable);

    /** Cancel a PENDING_PAYMENT order */
    OrderResponse cancelOrder(Long orderId, Long userId);

    /** Update order status — Admin only */
    OrderResponse updateOrderStatus(Long orderId,
                                    UpdateOrderStatusRequest request);

    /**
     * Check if a user has purchased a product with a given order status.
     * Called internally by customer-engagement-service via Feign.
     */
    boolean hasUserPurchasedProduct(Long userId, Long productId, String status);
}
