package com.runway.engagement.notifications.controller;

import com.runway.engagement.notifications.dto.InternalNotifyRequest;
import com.runway.engagement.notifications.model.NotificationEvent;
import com.runway.engagement.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal-only endpoint for service-to-service notification triggers.
 * Permitted without JWT — assumed trusted internal network call.
 */
@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/notify")
    public ResponseEntity<Void> notify(@RequestBody InternalNotifyRequest request) {
        try {
            NotificationEvent event = NotificationEvent.valueOf(request.getEvent());
            notificationService.notify(event, request.getUserId(), request.getPayload());
        } catch (IllegalArgumentException e) {
            log.warn("Internal notify: unknown event '{}' — skipped", request.getEvent());
        } catch (Exception e) {
            log.error("Internal notify failed for event '{}': {}", request.getEvent(), e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
