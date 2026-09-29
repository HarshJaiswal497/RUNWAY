package com.runway.order.repository;

import com.runway.order.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** Find specific item in a cart by product ID */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /** Check if product already in cart */
    boolean existsByCartIdAndProductId(Long cartId, Long productId);

    /** Delete specific item from cart */
    void deleteByCartIdAndProductId(Long cartId, Long productId);

    /** Delete all items from cart (on checkout or clear) */
    void deleteAllByCartId(Long cartId);
}
