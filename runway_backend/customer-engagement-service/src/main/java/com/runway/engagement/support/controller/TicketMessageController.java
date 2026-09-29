package com.runway.engagement.support.controller;

import com.runway.engagement.notifications.model.NotificationEvent;
import com.runway.engagement.notifications.service.NotificationService;
import com.runway.engagement.support.dto.TicketMessageMapper;
import com.runway.engagement.support.dto.TicketMessageRequest;
import com.runway.engagement.support.dto.TicketMessageResponse;
import com.runway.engagement.support.model.Ticket;
import com.runway.engagement.support.model.TicketMessage;
import com.runway.engagement.support.model.TicketMessageSenderRole;
import com.runway.engagement.support.service.TicketMessageService;
import com.runway.engagement.support.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
        name = "Support",
        description = "Ticket conversations and message exchange"
)
@RestController
@RequestMapping("/tickets/{ticketId}/messages")
public class TicketMessageController {

    private final TicketMessageService ticketMessageService;
    private final TicketService ticketService;
    private final NotificationService notificationService;

    public TicketMessageController(TicketMessageService ticketMessageService,
                                   TicketService ticketService,
                                   NotificationService notificationService) {
        this.ticketMessageService = ticketMessageService;
        this.ticketService = ticketService;
        this.notificationService = notificationService;
    }

    //  CUSTOMER / SUPPORT / ADMIN: send message
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SUPPORT') or hasRole('ADMIN')")
    @Operation(
            summary = "Send ticket message",
            description = "Adds a message to a ticket conversation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message sent"),
            @ApiResponse(responseCode = "403", description = "Not allowed to access this ticket")
    })
    public ResponseEntity<TicketMessageResponse> sendMessage(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            @Valid @RequestBody TicketMessageRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        boolean isCustomer = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

        // enforce ticket access (capture ticket for notification)
        Ticket ticket = ticketService.getTicketById(ticketId, userId, isCustomer);

        TicketMessageSenderRole senderRole = authentication.getAuthorities()
                .stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_SUPPORT")
                                || a.getAuthority().equals("ROLE_ADMIN")
                )
                ? TicketMessageSenderRole.SUPPORT
                : TicketMessageSenderRole.CUSTOMER;

        TicketMessage message = ticketMessageService.createMessage(
                TicketMessageMapper.toEntity(request, ticketId, userId, senderRole)
        );

        // Notify the customer when staff sends a reply
        if (senderRole == TicketMessageSenderRole.SUPPORT) {
            try {
                String raw = request.getMessage();
                String preview = raw.length() > 80 ? raw.substring(0, 80) + "…" : raw;
                notificationService.notify(
                    NotificationEvent.TICKET_REPLY,
                    ticket.getUserId(),
                    Map.of("ticketId", String.valueOf(ticketId), "preview", preview)
                );
            } catch (Exception e) {
                // notification failure must not block the message from being sent
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TicketMessageMapper.toResponse(message));
    }

    //  CUSTOMER / SUPPORT / ADMIN: view messages
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('SUPPORT') or hasRole('ADMIN')")
    @Operation(
            summary = "Get ticket messages",
            description = "Returns all messages for a ticket conversation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages retrieved"),
            @ApiResponse(responseCode = "403", description = "Not allowed to access this ticket")
    })
    public ResponseEntity<List<TicketMessageResponse>> getMessages(
            @Parameter(description = "Ticket ID") @PathVariable Long ticketId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        boolean isCustomer = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

        // enforce ticket access
        ticketService.getTicketById(ticketId, userId, isCustomer);

        return ResponseEntity.ok(
                ticketMessageService.getMessagesByTicketId(ticketId)
                        .stream()
                        .map(TicketMessageMapper::toResponse)
                        .toList()
        );
    }
}