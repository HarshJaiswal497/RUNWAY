package com.runway.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cart — JPA Entity
 *
 * One cart per user (UNIQUE on user_id).
 * Cart is auto-created when user first adds an item.
 * Cart is cleared after successful checkout.
 *
 * IMPORTANT: user_id here is the ID from Identity Service.
 * We store only the ID — not a @ManyToOne User object
 * because User is owned by Identity Service, not Order Service.
 * This is the microservices pattern — no cross-service JOINs.
 */
@Entity
@Table(name = "carts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "items")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID from Identity Service — no FK constraint across services
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @OneToMany(mappedBy = "cart",
               cascade  = CascadeType.ALL,
               orphanRemoval = true,
               fetch    = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

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

    // ── Computed helpers ──────────────────────────────────────

    public BigDecimal getTotal() {
        if (items == null || items.isEmpty()) return BigDecimal.ZERO;
        return items.stream()
            .map(CartItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public int getTotalItemCount() {
        if (items == null) return 0;
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }
}
