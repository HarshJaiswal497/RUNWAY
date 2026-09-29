package com.runway.engagement.support.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ticket")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long userId;
    private String subject;

    @Enumerated(EnumType.STRING)
    private TicketPriority priority; // can be low, med, high, critical

    @Enumerated(EnumType.STRING)
    private TicketStatus status; // open, in_progress, closed

    @Column(nullable = true)
    private Long assignedTo;

    private Long orderId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
