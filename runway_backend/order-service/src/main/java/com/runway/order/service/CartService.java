package com.runway.order.service;

import com.runway.order.util.Dtos.*;

/**
 * CartService — Interface
 * Called by CartController.
 * Never changes even when implementation changes.
 */
public interface CartService {

    /** Get cart for current user (creates one if it doesn't exist) */
    CartResponse getMyCart(Long userId);

    /** Add product to cart (updates quantity if already exists) */
    CartResponse addItem(Long userId, AddCartItemRequest request);

    /** Update quantity of an existing cart item */
    CartResponse updateItemQuantity(Long userId, Long productId,
                                    UpdateCartItemRequest request);

    /** Remove a specific product from cart */
    CartResponse removeItem(Long userId, Long productId);

    /** Remove all items from cart */
    CartResponse clearCart(Long userId);
}
