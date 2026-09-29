package com.runway.engagement.support;

import com.runway.engagement.support.model.TicketMessage;
import com.runway.engagement.support.repository.TicketMessageRepository;
import com.runway.engagement.support.service.TicketMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketMessageServiceTest {

    @Mock
    private TicketMessageRepository ticketMessageRepository;

    @InjectMocks
    private TicketMessageService service;

    // ------------------------------------------------
    // createMessage
    // ------------------------------------------------
    @Test
    void createMessage_savesAndReturnsMessage() {
        TicketMessage message = new TicketMessage();

        when(ticketMessageRepository.save(message))
                .thenReturn(message);

        TicketMessage result = service.createMessage(message);

        assertThat(result).isSameAs(message);
        verify(ticketMessageRepository).save(message);
    }

    // ------------------------------------------------
    // getMessagesByTicketId
    // ------------------------------------------------
    @Test
    void getMessagesByTicketId_returnsMessages() {
        TicketMessage message = new TicketMessage();
        message.setTicketId(99L);

        when(ticketMessageRepository.findByTicketId(99L))
                .thenReturn(List.of(message));

        List<TicketMessage> result =
                service.getMessagesByTicketId(99L);

        assertThat(result).hasSize(1);
        verify(ticketMessageRepository).findByTicketId(99L);
    }

    // ------------------------------------------------
    // deleteTicketMessages
    // ------------------------------------------------
    @Test
    void deleteTicketMessages_deletesAllMessagesForTicket() {
        when(ticketMessageRepository.deleteAllByTicketId(99L))
                .thenReturn(3);

        int deletedCount =
                service.deleteTicketMessages(99L);

        assertThat(deletedCount).isEqualTo(3);
        verify(ticketMessageRepository).deleteAllByTicketId(99L);
    }

    // ------------------------------------------------
    // deleteTicketMessage
    // ------------------------------------------------
    @Test
    void deleteTicketMessage_deletesSingleMessage() {
        when(ticketMessageRepository.deleteByTicketIdAndId(99L, 5L))
                .thenReturn(1);

        int deletedCount =
                service.deleteTicketMessage(99L, 5L);

        assertThat(deletedCount).isEqualTo(1);
        verify(ticketMessageRepository)
                .deleteByTicketIdAndId(99L, 5L);
    }
}