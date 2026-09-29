package com.runway.engagement.support.service;

import com.runway.engagement.support.model.TicketMessage;
import com.runway.engagement.support.repository.TicketMessageRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketMessageService {
    private final TicketMessageRepository ticketMessageRepository;

    public TicketMessageService(TicketMessageRepository ticketMessageRepository) {
        this.ticketMessageRepository = ticketMessageRepository;
    }

    // add a message
    public TicketMessage createMessage(TicketMessage ticketMessage) {
        return ticketMessageRepository.save(ticketMessage);
    }

    // get messages for ticket by id
    public List<TicketMessage> getMessagesByTicketId(Long ticketId) {
        return ticketMessageRepository.findByTicketId(ticketId);
    }

    @Transactional
    // delete all msgs in ticket chat
    public int deleteTicketMessages(Long ticketId) {
        return ticketMessageRepository.deleteAllByTicketId(ticketId);
    }

    @Transactional
    //delete only a msg
    public int deleteTicketMessage(Long ticketId, Long id) {
        return ticketMessageRepository.deleteByTicketIdAndId(ticketId, id);
    }
}
