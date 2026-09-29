package com.runway.engagement.support.controller;

import com.runway.engagement.notifications.model.NotificationEvent;
import com.runway.engagement.notifications.service.NotificationService;
import com.runway.engagement.support.dto.TicketMapper;
import com.runway.engagement.support.dto.TicketRequest;
import com.runway.engagement.support.dto.TicketResponse;
import com.runway.engagement.support.model.TicketStatus;
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
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
        name = "Support",
        description = "Support ticket lifecycle and management"
)
@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final NotificationService notificationService;

    public TicketController(TicketService ticketService,
                            NotificationService notificationService) {
        this.ticketService = ticketService;
        this.notificationService = notificationService;
    }

    //  CUSTOMER / ADMIN: create ticket
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(
            summary = "Create support ticket",
            description = "Creates a new support ticket with OPEN status"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket created"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody TicketRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        var ticket = ticketService.createTicket(
                TicketMapper.toEntity(request, userId)
        );

        notificationService.notify(
                NotificationEvent.TICKET_CREATED,
                userId,
                Map.of(
                        "ticketId", ticket.getId(),
                        "subject", ticket.getSubject()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TicketMapper.toResponse(ticket));
    }

    //  CUSTOMER: my tickets
    //  ADMIN / SUPPORT: all tickets
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('SUPPORT')")
    @Operation(
            summary = "Get tickets",
            description = "mine=true returns tickets created by the logged-in user. "
                    + "Admins and support can filter by status."
    )
    @ApiResponse(responseCode = "200", description = "Tickets retrieved")
    public ResponseEntity<List<TicketResponse>> getTickets(
            @Parameter(description = "Fetch only logged-in user's tickets", example = "false")
            @RequestParam(defaultValue = "false") boolean mine,
            @Parameter(description = "Filter tickets by status")
            @RequestParam(required = false) TicketStatus status,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        boolean isCustomer = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

        return ResponseEntity.ok(
                ticketService.getTickets(mine, status, userId, isCustomer)
                        .stream()
                        .map(TicketMapper::toResponse)
                        .toList()
        );
    }

    //  CUSTOMER / ADMIN / SUPPORT: ticket by id
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('SUPPORT')")
    @Operation(
            summary = "Get ticket by ID",
            description = "Returns ticket details if the user is authorized to access it"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket retrieved"),
            @ApiResponse(responseCode = "403", description = "Not allowed to access this ticket")
    })
    public ResponseEntity<TicketResponse> getTicketById(
            @Parameter(description = "Ticket ID") @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        boolean isCustomer = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

        return ResponseEntity.ok(
                TicketMapper.toResponse(
                        ticketService.getTicketById(id, userId, isCustomer)
                )
        );
    }

    //  ADMIN / SUPPORT: update ticket
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
    @Operation(
            summary = "Update ticket",
            description = "Assigns ticket to staff or updates ticket status"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition")
    })
    public ResponseEntity<TicketResponse> updateTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long id,
            @Parameter(description = "New ticket status")
            @RequestParam(required = false) TicketStatus status,
            @Parameter(description = "Explicitly assign to this staff user ID (admin use)")
            @RequestParam(required = false) Long assignedTo,
            Authentication authentication
    ) {
        Long staffId = (Long) authentication.getPrincipal();

        var ticket = ticketService.updateTicket(id, status, staffId, assignedTo);

        if (status == TicketStatus.CLOSED) {
            notificationService.notify(
                    NotificationEvent.TICKET_RESOLVED,
                    ticket.getUserId(),
                    Map.of(
                            "ticketId", ticket.getId(),
                            "resolutionSummary", "Your ticket has been resolved"
                    )
            );
        }

        return ResponseEntity.ok(
                TicketMapper.toResponse(ticket)
        );
    }

    //  ADMIN: delete ticket
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete ticket",
            description = "Deletes a ticket permanently"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ticket deleted"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ResponseEntity<Void> deleteTicket(
            @Parameter(description = "Ticket ID") @PathVariable Long id
    ) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}