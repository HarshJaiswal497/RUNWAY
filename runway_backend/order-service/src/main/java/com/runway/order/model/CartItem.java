package com.runway.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * CartItem — JPA Entity
 *
 * Represents one product line inside a cart.
 * Stores product snapshot (title + price at time of adding).
 * product_id links to Catalog Service — no cross-service FK.
 */
@Entity
@Table(name = "cart_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // ID from Catalog Service — no cross-service FK
    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Snapshot of product title at time of adding to cart
    @Column(name = "product_title", nullable = false, length = 255)
    private String productTitle;

    @Column(nullable = false)
    private int quantity;

    // Price at time of adding — locked so price changes don't affect cart
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    // ── Computed ──────────────────────────────────────────────

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
