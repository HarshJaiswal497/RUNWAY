package com.runway.engagement.support.repository;
import com.runway.engagement.support.model.TicketPriority;
import com.runway.engagement.support.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import com.runway.engagement.support.model.Ticket;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>{
    List<Ticket> findByUserId(Long userId);

    List<Ticket> findByAssignedTo(Long staffId);

    List<Ticket> findByPriority(TicketPriority priority);

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByUserIdAndStatus(Long userId, TicketStatus status);
}
