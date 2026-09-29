package com.runway.engagement.notifications.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "notification_template",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_template_event",
                        columnNames = {"event"}
                )
        }
)
public class NotificationTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    private String subject;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel; // can be EMAIL,SMS,PUSH

    @Lob
    private String content;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationEvent event;
}
