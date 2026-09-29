package com.runway.engagement.support.dto;

import com.runway.engagement.support.model.TicketMessageSenderRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class TicketMessageRequest {
    @NotBlank(message = "Message cannot be blank")
    private String message;
}
