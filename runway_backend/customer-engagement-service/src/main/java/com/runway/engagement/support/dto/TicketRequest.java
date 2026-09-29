package com.runway.engagement.support.dto;

import com.runway.engagement.support.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketRequest {
    @NotBlank(message = "Please enter the subject")
    private String subject;

    @NotNull(message = "Ticket priority is required")
    private TicketPriority priority;

    private Long orderId;
}
