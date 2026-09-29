package com.runway.engagement.reviews.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rating", uniqueConstraints = @UniqueConstraint(columnNames = {"productId", "userId"}))
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long productId;

    private long userId;
    private  int rating;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
