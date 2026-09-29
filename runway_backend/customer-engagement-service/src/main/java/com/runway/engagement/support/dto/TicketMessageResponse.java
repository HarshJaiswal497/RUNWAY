package com.runway.engagement.support.dto;

import com.runway.engagement.support.model.TicketMessageSenderRole;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonPropertyOrder({"id", "ticketId", "senderId", "senderRole","message","createdAt"})
public class TicketMessageResponse {
    private long id;
    private long ticketId;
    private long senderId;
    private String message;
    private TicketMessageSenderRole senderRole; // can be CUSTOMER, SUPPORT, SELLER, ADMIN
    private LocalDateTime createdAt;
}
