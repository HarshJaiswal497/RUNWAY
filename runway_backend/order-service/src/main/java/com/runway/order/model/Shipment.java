package com.runway.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Shipment — JPA Entity
 *
 * One shipment per order (UNIQUE on order_id).
 * Created automatically when order status changes to PROCESSING.
 * Manager/Admin updates status as shipment progresses.
 *
 * Status flow:
 *   PROCESSING → DISPATCHED → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
 *             ↘ RETURNED (if delivery fails and item comes back)
 */
@Entity
@Table(name = "shipments",
    indexes = {
        @Index(name = "idx_shipments_tracking", columnList = "tracking_number"),
        @Index(name = "idx_shipments_status",   columnList = "status")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(length = 100)
    private String carrier;

    @Column(name = "tracking_number", unique = true, length = 100)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.PROCESSING;

    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Status Enum ───────────────────────────────────────────

    public enum ShipmentStatus {
        PROCESSING,         // order confirmed, preparing to ship
        DISPATCHED,         // handed to carrier
        IN_TRANSIT,         // on the way
        OUT_FOR_DELIVERY,   // last-mile delivery
        DELIVERED,          // delivered to customer
        RETURNED            // returned to warehouse
    }
}
