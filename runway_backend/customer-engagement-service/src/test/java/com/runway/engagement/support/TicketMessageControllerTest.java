package com.runway.engagement.support;

import com.runway.engagement.support.controller.TicketMessageController;
import com.runway.engagement.support.dto.TicketMessageRequest;
import com.runway.engagement.support.model.Ticket;
import com.runway.engagement.support.model.TicketMessage;
import com.runway.engagement.support.service.TicketMessageService;
import com.runway.engagement.support.service.TicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketMessageControllerTest {

    @Mock
    private TicketMessageService ticketMessageService;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketMessageController controller;

    // ------------------------------------------------
    // POST /tickets/{ticketId}/messages
    // ------------------------------------------------
    @Test
    void sendMessage_returns201_andMessage() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        Collection<GrantedAuthority> authorities =
                List.of((GrantedAuthority) () -> "ROLE_CUSTOMER");
        doReturn(authorities).when(auth).getAuthorities();

        Ticket ticket = new Ticket();
        ticket.setId(99L);
        ticket.setUserId(10L);

        when(ticketService.getTicketById(99L, 10L, true))
                .thenReturn(ticket);

        TicketMessageRequest request = new TicketMessageRequest();

        TicketMessage saved = new TicketMessage();
        saved.setId(1L);
        saved.setTicketId(99L);
        saved.setSenderId(10L);

        when(ticketMessageService.createMessage(any(TicketMessage.class)))
                .thenReturn(saved);

        var response =
                controller.sendMessage(99L, request, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        verify(ticketService).getTicketById(99L, 10L, true);
        verify(ticketMessageService).createMessage(any(TicketMessage.class));
    }

    // ------------------------------------------------
    // GET /tickets/{ticketId}/messages
    // ------------------------------------------------
    @Test
    void getMessages_returnsMessages() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);

        Collection<GrantedAuthority> authorities =
                List.of((GrantedAuthority) () -> "ROLE_CUSTOMER");
        doReturn(authorities).when(auth).getAuthorities();

        Ticket ticket = new Ticket();
        ticket.setId(99L);
        ticket.setUserId(10L);

        when(ticketService.getTicketById(99L, 10L, true))
                .thenReturn(ticket);

        TicketMessage message = new TicketMessage();
        message.setId(1L);
        message.setTicketId(99L);
        message.setSenderId(10L);

        when(ticketMessageService.getMessagesByTicketId(99L))
                .thenReturn(List.of(message));

        var response =
                controller.getMessages(99L, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        verify(ticketService).getTicketById(99L, 10L, true);
        verify(ticketMessageService).getMessagesByTicketId(99L);
    }
}
