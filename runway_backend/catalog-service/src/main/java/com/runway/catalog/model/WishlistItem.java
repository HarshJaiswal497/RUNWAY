package com.runway.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "product_id"})
        },
        indexes = {
                @Index(name = "idx_wishlist_user_id", columnList = "user_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}