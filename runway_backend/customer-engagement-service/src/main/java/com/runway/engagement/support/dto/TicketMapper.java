package com.runway.engagement.support.dto;

import com.runway.engagement.support.model.Ticket;
import com.runway.engagement.support.model.TicketStatus;

public class TicketMapper {

    public static Ticket toEntity(
            TicketRequest request,
            Long userId
    ) {
        Ticket ticket = new Ticket();
        ticket.setUserId(userId);
        ticket.setOrderId(request.getOrderId());
        ticket.setSubject(request.getSubject());
        ticket.setPriority(request.getPriority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setAssignedTo(null);
        return ticket;
    }

    public static TicketResponse toResponse(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.setId(ticket.getId());
        response.setUserId(ticket.getUserId());
        response.setOrderId(ticket.getOrderId());
        response.setSubject(ticket.getSubject());
        response.setPriority(ticket.getPriority());
        response.setStatus(ticket.getStatus());
        response.setAssignedTo(ticket.getAssignedTo());
        response.setCreatedAt(ticket.getCreatedAt());
        return response;
    }
}