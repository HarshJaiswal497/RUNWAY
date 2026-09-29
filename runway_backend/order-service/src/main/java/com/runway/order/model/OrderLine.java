package com.runway.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * OrderLine — JPA Entity
 *
 * Snapshot of each product at the time the order was placed.
 * Even if product price changes later, OrderLine keeps the original price.
 * This is intentional — "what you paid is what you paid".
 *
 * product_id links to Catalog Service — no cross-service FK.
 */
@Entity
@Table(name = "order_lines",
    indexes = {
        @Index(name = "idx_order_lines_order",   columnList = "order_id"),
        @Index(name = "idx_order_lines_product", columnList = "product_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Product ID from Catalog Service
    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Snapshot of product title at time of order
    @Column(name = "product_title", nullable = false, length = 255)
    private String productTitle;

    @Column(nullable = false)
    private int quantity;

    // Snapshot of price at time of order
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    // Auto-calculates lineTotal before saving
    @PrePersist
    @PreUpdate
    protected void computeLineTotal() {
        if (unitPrice != null && quantity > 0) {
            lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
