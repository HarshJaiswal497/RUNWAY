package com.runway.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order — JPA Entity
 *
 * Created when user checks out their cart.
 * Each order has a unique order_number (ORD-XXXXXXXX).
 *
 * Status flow:
 *   PENDING_PAYMENT → PAID → PROCESSING → SHIPPED → DELIVERED → COMPLETED
 *                          ↘ CANCELLED (only from PENDING_PAYMENT)
 *                          ↘ REFUNDED  (only after PAID)
 *
 * user_id is from Identity Service — no cross-service FK.
 */
@Entity
@Table(name = "orders",
    indexes = {
        @Index(name = "idx_orders_user_id",  columnList = "user_id"),
        @Index(name = "idx_orders_status",   columnList = "status"),
        @Index(name = "idx_orders_created",  columnList = "created_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "lines")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    // User ID from Identity Service
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "order",
               cascade  = CascadeType.ALL,
               orphanRemoval = true,
               fetch    = FetchType.LAZY)
    @Builder.Default
    private List<OrderLine> lines = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (orderNumber == null) {
            orderNumber = "ORD-" +
                UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Status Enum ───────────────────────────────────────────

    public enum OrderStatus {
        PENDING_PAYMENT,   // order created, waiting for payment
        PAID,              // payment received
        PROCESSING,        // being prepared/packed
        SHIPPED,           // handed to carrier
        DELIVERED,         // delivered to customer
        COMPLETED,         // customer confirmed receipt
        CANCELLED,         // cancelled before payment
        REFUNDED           // refunded after payment
    }

    // ── Helper methods ────────────────────────────────────────

    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING_PAYMENT;
    }

    public boolean isPaid() {
        return status != OrderStatus.PENDING_PAYMENT
            && status != OrderStatus.CANCELLED;
    }
}
