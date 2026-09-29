package com.runway.order.service;

import com.runway.order.client.CatalogServiceClient;
import com.runway.order.dto.catalog.ProductDTO;
import com.runway.order.exception.ResourceNotFoundException;
import com.runway.order.exception.ValidationException;
import com.runway.order.model.Cart;
import com.runway.order.model.CartItem;
import com.runway.order.repository.CartItemRepository;
import com.runway.order.repository.CartRepository;
import com.runway.order.util.Dtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CartServiceImpl — Business Logic for Cart
 *
 * Uses Feign Client to call Catalog Service for:
 *   - Product validation (does the product exist?)
 *   - Real price fetching (never trust client-supplied price)
 *   - Stock availability check
 *
 * Every method runs inside a DB transaction.
 * If any step fails, all DB changes are rolled back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository        cartRepository;
    private final CartItemRepository    cartItemRepository;
    private final CatalogServiceClient  catalogClient;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getMyCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return CartResponse.from(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        // 1. Validate quantity
        if (request.getQuantity() <= 0)
            throw new ValidationException("Quantity must be at least 1.");

        // 2. Feign call: Validate product exists and get real price from Catalog Service
        ProductDTO product;
        try {
            product = catalogClient.getProductById(request.getProductId());
        } catch (Exception e) {
            log.error("Failed to fetch product {} from Catalog Service: {}",
                    request.getProductId(), e.getMessage());
            throw new ValidationException(
                    "Product not found in catalog: " + request.getProductId());
        }

        // 3. Feign call: Check stock availability
        Integer stock;
        try {
            stock = catalogClient.getStock(request.getProductId());
        } catch (Exception e) {
            log.error("Failed to check stock for product {}: {}",
                    request.getProductId(), e.getMessage());
            throw new ValidationException(
                    "Unable to verify stock for product: " + request.getProductId());
        }

        if (stock < request.getQuantity()) {
            throw new ValidationException(
                    "Insufficient stock for '" + product.getTitle()
                            + "'. Available: " + stock + ", Requested: " + request.getQuantity());
        }

        // 4. Use verified data from Catalog Service
        Cart cart = getOrCreateCart(userId);

        // If product already in cart — increase quantity
        cartItemRepository.findByCartIdAndProductId(
                        cart.getId(), request.getProductId())
                .ifPresentOrElse(
                        existingItem -> {
                            int newQty = existingItem.getQuantity() + request.getQuantity();
                            if (newQty > 100)
                                throw new ValidationException(
                                        "Total quantity cannot exceed 100 for one product.");
                            if (newQty > stock)
                                throw new ValidationException(
                                        "Insufficient stock. Available: " + stock
                                                + ", Total in cart would be: " + newQty);
                            existingItem.setQuantity(newQty);
                            existingItem.setUnitPrice(product.getPrice()); // refresh price
                            existingItem.setProductTitle(product.getTitle()); // refresh title
                            cartItemRepository.save(existingItem);
                            log.debug("Updated cart item qty: productId={}, newQty={}",
                                    request.getProductId(), newQty);
                        },
                        () -> {
                            // Product not in cart — add new item with verified data
                            CartItem newItem = CartItem.builder()
                                    .cart(cart)
                                    .productId(request.getProductId())
                                    .productTitle(product.getTitle())     // from Catalog
                                    .quantity(request.getQuantity())
                                    .unitPrice(product.getPrice())        // from Catalog
                                    .build();
                            cartItemRepository.save(newItem);
                            log.debug("Added new cart item: productId={}, title={}, price={}",
                                    request.getProductId(), product.getTitle(), product.getPrice());
                        }
                );

        // Reload cart with updated items
        Cart updatedCart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow();
        return CartResponse.from(updatedCart);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId,
                                           UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found in cart: " + productId));

        // Feign call: Check stock for updated quantity
        try {
            Integer stock = catalogClient.getStock(productId);
            if (stock < request.getQuantity()) {
                throw new ValidationException(
                        "Insufficient stock. Available: " + stock
                                + ", Requested: " + request.getQuantity());
            }
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            log.warn("Stock check failed for product {}, allowing update: {}",
                    productId, e.getMessage());
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        log.debug("Updated cart item: productId={}, qty={}",
                productId, request.getQuantity());

        Cart updatedCart = cartRepository.findByUserIdWithItems(userId).orElseThrow();
        return CartResponse.from(updatedCart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (!cartItemRepository.existsByCartIdAndProductId(cart.getId(), productId)) {
            throw new ResourceNotFoundException(
                    "Product not found in cart: " + productId);
        }

        cartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId);
        log.debug("Removed cart item: userId={}, productId={}", userId, productId);

        Cart updatedCart = cartRepository.findByUserIdWithItems(userId).orElseThrow();
        return CartResponse.from(updatedCart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cartItemRepository.deleteAllByCartId(cart.getId());
        log.debug("Cleared cart: userId={}", userId);

        Cart updatedCart = cartRepository.findByUserIdWithItems(userId).orElseThrow();
        return CartResponse.from(updatedCart);
    }

    /**
     * Gets existing cart or creates a new empty one.
     */
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .build();
                    Cart saved = cartRepository.save(newCart);
                    log.debug("Created new cart for userId={}", userId);
                    return saved;
                });
    }
}
