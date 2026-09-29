//package com.runway.engagement.support;
//
//import com.runway.engagement.notifications.model.NotificationEvent;
//import com.runway.engagement.notifications.service.NotificationService;
//import com.runway.engagement.support.controller.TicketController;
//import com.runway.engagement.support.dto.TicketRequest;
//import com.runway.engagement.support.model.Ticket;
//import com.runway.engagement.support.model.TicketPriority;
//import com.runway.engagement.support.model.TicketStatus;
//import com.runway.engagement.support.service.TicketService;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.GrantedAuthority;
//
//import java.util.Collection;
//import java.util.List;
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class TicketControllerTest {
//
//    @Mock
//    private TicketService ticketService;
//
//    @Mock
//    private NotificationService notificationService;
//
//    @InjectMocks
//    private TicketController controller;
//
//    // POST /tickets
//    @Test
//    void createTicket_returns201_andSendsNotification() {
//        Authentication auth = mock(Authentication.class);
//        when(auth.getPrincipal()).thenReturn(10L);
//
//        TicketRequest request = new TicketRequest();
//        request.setSubject("Order issue");
//        request.setPriority(TicketPriority.HIGH);
//
//        Ticket saved = new Ticket();
//        saved.setId(1L);
//        saved.setUserId(10L);
//        saved.setSubject("Order issue");
//        saved.setStatus(TicketStatus.OPEN);
//
//        when(ticketService.createTicket(any(Ticket.class)))
//                .thenReturn(saved);
//
//        var response = controller.createTicket(request, auth);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//        assertThat(response.getBody()).isNotNull();
//
//        verify(ticketService).createTicket(any(Ticket.class));
//        verify(notificationService).notify(
//                NotificationEvent.TICKET_CREATED,
//                10L,
//                Map.of(
//                        "ticketId", 1L,
//                        "subject", "Order issue"
//                )
//        );
//    }
//
//    // GET /tickets?mine=true
//    @Test
//    void getTickets_mineTrue_returnsUserTickets() {
//        Authentication auth = mock(Authentication.class);
//        when(auth.getPrincipal()).thenReturn(10L);
//
//        Collection<GrantedAuthority> authorities =
//                List.of((GrantedAuthority) () -> "ROLE_CUSTOMER");
//        doReturn(authorities).when(auth).getAuthorities();
//
//        Ticket ticket = new Ticket();
//        ticket.setId(1L);
//        ticket.setUserId(10L);
//        ticket.setStatus(TicketStatus.OPEN);
//
//        when(ticketService.getTickets(true, null, 10L, true))
//                .thenReturn(List.of(ticket));
//
//        var response = controller.getTickets(true, null, auth);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).hasSize(1);
//
//        verify(ticketService)
//                .getTickets(true, null, 10L, true);
//    }
//
//    // GET /tickets?status=OPEN (ADMIN)
//    @Test
//    void getTickets_statusFilter_adminReturnsFiltered() {
//        Authentication auth = mock(Authentication.class);
//        when(auth.getPrincipal()).thenReturn(99L);
//
//        Collection<GrantedAuthority> authorities =
//                List.of((GrantedAuthority) () -> "ROLE_ADMIN");
//        doReturn(authorities).when(auth).getAuthorities();
//
//        Ticket ticket = new Ticket();
//        ticket.setStatus(TicketStatus.OPEN);
//
//        when(ticketService.getTickets(false, TicketStatus.OPEN, 99L, false))
//                .thenReturn(List.of(ticket));
//
//        var response = controller.getTickets(false, TicketStatus.OPEN, auth);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).hasSize(1);
//
//        verify(ticketService)
//                .getTickets(false, TicketStatus.OPEN, 99L, false);
//    }
//
//    // GET /tickets/{id}
//    @Test
//    void getTicketById_returnsTicket() {
//        Authentication auth = mock(Authentication.class);
//        when(auth.getPrincipal()).thenReturn(10L);
//
//        Collection<GrantedAuthority> authorities =
//                List.of((GrantedAuthority) () -> "ROLE_CUSTOMER");
//        doReturn(authorities).when(auth).getAuthorities();
//
//        Ticket ticket = new Ticket();
//        ticket.setId(5L);
//        ticket.setUserId(10L);
//
//        when(ticketService.getTicketById(5L, 10L, true))
//                .thenReturn(ticket);
//
//        var response = controller.getTicketById(5L, auth);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody()).isNotNull();
//
//        verify(ticketService).getTicketById(5L, 10L, true);
//    }
//
//    // PUT /tickets/{id} (CLOSED)
//    @Test
//    void updateTicket_closed_sendsResolutionNotification() {
//        Authentication auth = mock(Authentication.class);
//        when(auth.getPrincipal()).thenReturn(20L);
//
//        Ticket closed = new Ticket();
//        closed.setId(4L);
//        closed.setUserId(10L);
//        closed.setStatus(TicketStatus.CLOSED);
//
//        when(ticketService.updateTicket(4L, TicketStatus.CLOSED, 20L))
//                .thenReturn(closed);
//
//        var response = controller.updateTicket(4L, TicketStatus.CLOSED, auth);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//
//        verify(ticketService)
//                .updateTicket(4L, TicketStatus.CLOSED, 20L);
//        verify(notificationService).notify(
//                NotificationEvent.TICKET_RESOLVED,
//                10L,
//                Map.of(
//                        "ticketId", 4L,
//                        "resolutionSummary", "Your ticket has been resolved"
//                )
//        );
//    }
//
//    // DELETE /tickets/{id}
//    @Test
//    void deleteTicket_returns204() {
//        var response = controller.deleteTicket(9L);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
//
//        verify(ticketService).deleteTicket(9L);
//    }
//}