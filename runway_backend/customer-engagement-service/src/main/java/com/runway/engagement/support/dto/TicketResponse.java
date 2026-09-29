package com.runway.engagement.support.dto;

import com.runway.engagement.support.model.TicketPriority;
import com.runway.engagement.support.model.TicketStatus;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@JsonPropertyOrder({
        "id", "userId", "orderId",
        "subject", "priority",
        "status", "assignedTo",
        "createdAt"
})
public class TicketResponse {
    private Long id;
    private Long userId;
    private Long orderId;
    private String subject;
    private TicketPriority priority;
    private TicketStatus status;
    private Long assignedTo;
    private LocalDateTime createdAt;
}