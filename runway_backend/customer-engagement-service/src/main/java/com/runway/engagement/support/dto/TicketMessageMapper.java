package com.runway.engagement.support.dto;

import com.runway.engagement.support.model.TicketMessage;
import com.runway.engagement.support.model.TicketMessageSenderRole;

public class TicketMessageMapper {

    public static TicketMessage toEntity(
            TicketMessageRequest request,
            Long ticketId,
            Long senderId,
            TicketMessageSenderRole senderRole
    ) {
        TicketMessage msg = new TicketMessage();
        msg.setTicketId(ticketId);
        msg.setSenderId(senderId);
        msg.setSenderRole(senderRole);
        msg.setMessage(request.getMessage());
        return msg;
    }

    public static TicketMessageResponse toResponse(TicketMessage msg) {
        TicketMessageResponse response = new TicketMessageResponse();
        response.setId(msg.getId());
        response.setTicketId(msg.getTicketId());
        response.setSenderId(msg.getSenderId());
        response.setSenderRole(msg.getSenderRole());
        response.setMessage(msg.getMessage());
        response.setCreatedAt(msg.getCreatedAt());
        return response;
    }
}