package com.runway.engagement.support.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.runway.engagement.support.model.TicketMessage;

import java.util.List;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {
    List<TicketMessage> findByTicketId(Long ticketId);

    int deleteAllByTicketId(Long ticketId);

    int deleteByTicketIdAndId(Long ticketId, Long id);
}
