//package com.runway.engagement.support;
//
//import com.runway.engagement.support.exception.TicketNotFoundException;
//import com.runway.engagement.support.model.Ticket;
//import com.runway.engagement.support.model.TicketStatus;
//import com.runway.engagement.support.repository.TicketRepository;
//import com.runway.engagement.support.service.TicketService;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.access.AccessDeniedException;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class TicketServiceTest {
//
//    @Mock
//    private TicketRepository ticketRepository;
//
//    @InjectMocks
//    private TicketService service;
//
//    // ------------------------------------------------
//    // createTicket
//    // ------------------------------------------------
//    @Test
//    void createTicket_setsStatusAndSaves() {
//        Ticket ticket = new Ticket();
//
//        when(ticketRepository.save(ticket))
//                .thenReturn(ticket);
//
//        Ticket result = service.createTicket(ticket);
//
//        assertThat(result.getStatus()).isEqualTo(TicketStatus.OPEN);
//        verify(ticketRepository).save(ticket);
//    }
//
//    // ------------------------------------------------
//    // getTickets - CUSTOMER (mine)
//    // ------------------------------------------------
//    @Test
//    void getTickets_customerMine_returnsUserTickets() {
//        when(ticketRepository.findByUserId(10L))
//                .thenReturn(List.of(new Ticket()));
//
//        List<Ticket> result =
//                service.getTickets(true, null, 10L, true);
//
//        assertThat(result).hasSize(1);
//        verify(ticketRepository).findByUserId(10L);
//    }
//
//    // ------------------------------------------------
//    // getTickets - CUSTOMER (mine + status)
//    // ------------------------------------------------
//    @Test
//    void getTickets_customerWithStatus_returnsFiltered() {
//        when(ticketRepository.findByUserIdAndStatus(10L, TicketStatus.OPEN))
//                .thenReturn(List.of(new Ticket()));
//
//        List<Ticket> result =
//                service.getTickets(true, TicketStatus.OPEN, 10L, true);
//
//        assertThat(result).hasSize(1);
//        verify(ticketRepository)
//                .findByUserIdAndStatus(10L, TicketStatus.OPEN);
//    }
//
//    // ------------------------------------------------
//    // getTickets - ADMIN (all)
//    // ------------------------------------------------
//    @Test
//    void getTickets_adminAll_returnsAllTickets() {
//        when(ticketRepository.findAll())
//                .thenReturn(List.of(new Ticket()));
//
//        List<Ticket> result =
//                service.getTickets(false, null, 99L, false);
//
//        assertThat(result).hasSize(1);
//        verify(ticketRepository).findAll();
//    }
//
//    // ------------------------------------------------
//    // getTickets - ADMIN (status)
//    // ------------------------------------------------
//    @Test
//    void getTickets_adminWithStatus_returnsFiltered() {
//        when(ticketRepository.findByStatus(TicketStatus.OPEN))
//                .thenReturn(List.of(new Ticket()));
//
//        List<Ticket> result =
//                service.getTickets(false, TicketStatus.OPEN, 99L, false);
//
//        assertThat(result).hasSize(1);
//        verify(ticketRepository).findByStatus(TicketStatus.OPEN);
//    }
//
//    // ------------------------------------------------
//    // getTicketById - customer own ticket
//    // ------------------------------------------------
//    @Test
//    void getTicketById_customerOwnTicket_allowed() {
//        Ticket ticket = new Ticket();
//        ticket.setUserId(10L);
//
//        when(ticketRepository.findById(1L))
//                .thenReturn(Optional.of(ticket));
//
//        Ticket result =
//                service.getTicketById(1L, 10L, true);
//
//        assertThat(result).isSameAs(ticket);
//    }
//
//    // ------------------------------------------------
//    // getTicketById - customer forbidden
//    // ------------------------------------------------
//    @Test
//    void getTicketById_customerNotOwner_denied() {
//        Ticket ticket = new Ticket();
//        ticket.setUserId(20L);
//
//        when(ticketRepository.findById(1L))
//                .thenReturn(Optional.of(ticket));
//
//        assertThatThrownBy(() ->
//                service.getTicketById(1L, 10L, true)
//        ).isInstanceOf(AccessDeniedException.class);
//    }
//
//    // ------------------------------------------------
//    // getTicketById - admin allowed
//    // ------------------------------------------------
//    @Test
//    void getTicketById_adminAllowed() {
//        Ticket ticket = new Ticket();
//        ticket.setUserId(20L);
//
//        when(ticketRepository.findById(1L))
//                .thenReturn(Optional.of(ticket));
//
//        Ticket result =
//                service.getTicketById(1L, 99L, false);
//
//        assertThat(result).isSameAs(ticket);
//    }
//
//    // ------------------------------------------------
//    // updateTicket - assign + status
//    // ------------------------------------------------
//    @Test
//    void updateTicket_assignsAndUpdatesStatus() {
//        Ticket ticket = new Ticket();
//        ticket.setStatus(TicketStatus.OPEN);
//
//        when(ticketRepository.findById(1L))
//                .thenReturn(Optional.of(ticket));
//        when(ticketRepository.save(ticket))
//                .thenReturn(ticket);
//
//        Ticket result =
//                service.updateTicket(1L, TicketStatus.IN_PROGRESS, 20L);
//
//        assertThat(result.getAssignedTo()).isEqualTo(20L);
//        assertThat(result.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
//    }
//
//    // ------------------------------------------------
//    // updateTicket - closed ticket cannot change
//    // ------------------------------------------------
//    @Test
//    void updateTicket_closedTicket_throwsException() {
//        Ticket ticket = new Ticket();
//        ticket.setStatus(TicketStatus.CLOSED);
//
//        when(ticketRepository.findById(1L))
//                .thenReturn(Optional.of(ticket));
//
//        assertThatThrownBy(() ->
//                service.updateTicket(1L, TicketStatus.OPEN, 20L)
//        ).isInstanceOf(IllegalStateException.class);
//    }
//
//    // ------------------------------------------------
//    // deleteTicket - success
//    // ------------------------------------------------
//    @Test
//    void deleteTicket_success() {
//        when(ticketRepository.existsById(9L))
//                .thenReturn(true);
//
//        service.deleteTicket(9L);
//
//        verify(ticketRepository).deleteById(9L);
//    }
//
//    // ------------------------------------------------
//    // deleteTicket - not found
//    // ------------------------------------------------
//    @Test
//    void deleteTicket_notFound() {
//        when(ticketRepository.existsById(9L))
//                .thenReturn(false);
//
//        assertThatThrownBy(() ->
//                service.deleteTicket(9L)
//        ).isInstanceOf(TicketNotFoundException.class);
//
//        verify(ticketRepository, never()).deleteById(any());
//    }
//}