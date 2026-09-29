package com.runway.engagement.support.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
public class TicketMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long ticketId;
    private long senderId;

    @Lob
    private String message;

    @Enumerated(EnumType.STRING)
    private TicketMessageSenderRole senderRole; // can be CUSTOMER, SUPPORT, SELLER, ADMIN

    @CreationTimestamp
    private LocalDateTime createdAt;
}
