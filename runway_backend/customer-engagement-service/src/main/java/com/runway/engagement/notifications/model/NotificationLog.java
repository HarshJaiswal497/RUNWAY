package com.runway.engagement.notifications.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationEvent event;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    private NotificationTemplate template;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel; // EMAIL, SMS, PUSH

    @Enumerated(EnumType.STRING)
    private NotificationStatus status; // QUEUED, SENT, FAILED

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
    }
}