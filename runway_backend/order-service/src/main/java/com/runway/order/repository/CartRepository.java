package com.runway.order.repository;

import com.runway.order.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * CartRepository — Spring Data JPA
 *
 * Spring generates all SQL from method names automatically.
 * findByUserId → SELECT * FROM carts WHERE user_id = ?
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Find cart by user ID.
     * Returns empty Optional if user has no cart yet.
     */
    Optional<Cart> findByUserId(Long userId);

    /**
     * Check if cart exists for user.
     */
    boolean existsByUserId(Long userId);

    /**
     * Load cart WITH all items in one query (avoids N+1 problem).
     * FETCH JOIN loads items eagerly even though FetchType is LAZY.
     */
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.userId = :userId")
    Optional<Cart> findByUserIdWithItems(Long userId);
}
