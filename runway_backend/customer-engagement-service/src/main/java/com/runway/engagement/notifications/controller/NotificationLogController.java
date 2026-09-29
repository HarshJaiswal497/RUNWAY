package com.runway.engagement.notifications.controller;

import com.runway.engagement.notifications.dto.NotificationLogMapper;
import com.runway.engagement.notifications.dto.NotificationLogRequest;
import com.runway.engagement.notifications.dto.NotificationLogResponse;
import com.runway.engagement.notifications.model.NotificationStatus;
import com.runway.engagement.notifications.service.NotificationLogService;
import com.runway.engagement.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Notifications",
        description = "Notification dispatch and delivery logs"
)
@RestController
@RequestMapping("/notification-logs")
public class NotificationLogController {

    private final NotificationLogService notificationLogService;
    private final NotificationService notificationService;

    public NotificationLogController(
            NotificationLogService notificationLogService,
            NotificationService notificationService
    ) {
        this.notificationLogService = notificationLogService;
        this.notificationService = notificationService;
    }

    //  ADMIN / SUPPORT: simulate notification
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
    @Operation(
            summary = "Trigger notification",
            description = "Triggers a notification event for a user (admin/support only)"
    )
    @ApiResponse(responseCode = "201", description = "Notification triggered")
    public ResponseEntity<Void> logNotification(
            @Valid @RequestBody NotificationLogRequest request
    ) {
        notificationService.notify(
                request.getEvent(),
                request.getUserId(),
                request.getPayload()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //  CUSTOMER: my logs
    //  ADMIN / SUPPORT: all logs
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('SUPPORT')")
    @Operation(
            summary = "Get notification logs",
            description = "mine=true returns logs for logged-in user. "
                    + "Admins/support may filter by status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logs retrieved"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<NotificationLogResponse>> getLogs(
            @Parameter(description = "Fetch only logged-in user's logs", example = "false")
            @RequestParam(defaultValue = "false") boolean mine,
            @Parameter(description = "Filter logs by status")
            @RequestParam(required = false) NotificationStatus status,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        boolean isCustomer = authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

        return ResponseEntity.ok(
                notificationLogService.getLogs(mine, status, userId, isCustomer)
                        .stream()
                        .map(NotificationLogMapper::toResponse)
                        .toList()
        );
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN') or hasRole('SUPPORT')")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationLogService.markRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        notificationLogService.markAllRead(userId);
        return ResponseEntity.ok().build();
    }

    //  ADMIN / SUPPORT: log by id
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
    @Operation(
            summary = "Get notification log by ID",
            description = "Returns a single notification delivery log"
    )
    @ApiResponse(responseCode = "200", description = "Log retrieved")
    public ResponseEntity<NotificationLogResponse> getLogById(
            @Parameter(description = "Notification log ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                NotificationLogMapper.toResponse(
                        notificationLogService.getLogById(id)
                )
        );
    }
}