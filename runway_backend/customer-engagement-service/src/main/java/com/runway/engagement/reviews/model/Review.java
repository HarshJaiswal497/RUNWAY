package com.runway.engagement.reviews.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "review",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_user_product",
                        columnNames = {"user_id", "product_id"}
                )
        }
)
public class Review {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "product_id")
    private long productId;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "rating")
    private int rating;

    @Column(name = "title")
    private String title;

    @Column(name = "body", columnDefinition = "LONGTEXT")
    @Lob
    private String body;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ReviewStatus status; // can be Pending, Approved, Rejected

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
}
