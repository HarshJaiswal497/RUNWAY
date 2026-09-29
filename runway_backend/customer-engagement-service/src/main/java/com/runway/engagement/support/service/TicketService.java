package com.runway.engagement.support.service;

import com.runway.engagement.notifications.model.NotificationEvent;
import com.runway.engagement.notifications.service.NotificationService;
import com.runway.engagement.support.exception.TicketNotFoundException;
import com.runway.engagement.support.model.Ticket;
import com.runway.engagement.support.model.TicketMessage;
import com.runway.engagement.support.model.TicketMessageSenderRole;
import com.runway.engagement.support.model.TicketStatus;
import com.runway.engagement.support.repository.TicketRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    private static final String CANNED_ACK =
        "Thank you for reaching out to RunWay Client Services. " +
        "Your enquiry has been received and a dedicated advisor has been assigned to your case. " +
        "We will be in touch with you shortly.";

    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;
    private final TicketMessageService ticketMessageService;

    public TicketService(TicketRepository ticketRepository,
                         NotificationService notificationService,
                         TicketMessageService ticketMessageService) {
        this.ticketRepository = ticketRepository;
        this.notificationService = notificationService;
        this.ticketMessageService = ticketMessageService;
    }

    //  create ticket
    public Ticket createTicket(Ticket ticket) {
        ticket.setStatus(TicketStatus.OPEN);
        return ticketRepository.save(ticket);
    }

    //  fetch tickets (mine / status)
    public List<Ticket> getTickets(
            boolean mine,
            TicketStatus status,
            Long userId,
            boolean isCustomer
    ) {
        // CUSTOMER: always scoped to own tickets
        if (isCustomer) {
            if (status != null) {
                return ticketRepository.findByUserIdAndStatus(userId, status);
            }
            return ticketRepository.findByUserId(userId);
        }

        // ADMIN / SUPPORT
        if (status != null) {
            return ticketRepository.findByStatus(status);
        }

        return ticketRepository.findAll();
    }

    //  get by id
    public Ticket getTicketById(Long id, Long userId, boolean isCustomer) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found"));

        if (isCustomer && ticket.getUserId() != userId) {
            throw new AccessDeniedException("Not allowed to access this ticket");
        }

        return ticket;
    }

    //  update ticket (assign / status)
    public Ticket updateTicket(Long id, TicketStatus status, Long staffId, Long assignedTo) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found"));

        if (status != null) {
            if (ticket.getStatus() == TicketStatus.CLOSED) {
                throw new IllegalStateException("Closed tickets cannot be modified");
            }
            ticket.setStatus(status);
        }

        boolean wasUnassigned = ticket.getAssignedTo() == null;

        if (assignedTo != null) {
            ticket.setAssignedTo(assignedTo);
        } else if (wasUnassigned) {
            ticket.setAssignedTo(staffId);
        }

        Ticket saved = ticketRepository.save(ticket);

        // On first assignment: send canned acknowledgment + notify customer
        if (wasUnassigned && saved.getAssignedTo() != null) {
            TicketMessage ack = new TicketMessage();
            ack.setTicketId(saved.getId());
            ack.setSenderId(saved.getAssignedTo());
            ack.setSenderRole(TicketMessageSenderRole.SUPPORT);
            ack.setMessage(CANNED_ACK);
            ticketMessageService.createMessage(ack);

            try {
                notificationService.notify(
                    NotificationEvent.TICKET_ASSIGNED,
                    saved.getUserId(),
                    Map.of("ticketId", String.valueOf(saved.getId()))
                );
            } catch (Exception e) {
                // notification failure must not break ticket update
            }
        }

        return saved;
    }

    //  delete ticket
    public void deleteTicket(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new TicketNotFoundException("Ticket not found");
        }
        ticketRepository.deleteById(id);
    }
}