package com.runway.engagement.notifications.controller;

import com.runway.engagement.notifications.dto.NotificationTemplateMapper;
import com.runway.engagement.notifications.dto.NotificationTemplateRequest;
import com.runway.engagement.notifications.dto.NotificationTemplateResponse;
import com.runway.engagement.notifications.model.NotificationChannel;
import com.runway.engagement.notifications.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Hidden
@RequestMapping("/notification-templates")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    public NotificationTemplateController(NotificationTemplateService notificationTemplateService) {
        this.notificationTemplateService = notificationTemplateService;
    }

    @PostMapping
    public ResponseEntity<NotificationTemplateResponse> createTemplate(
            @Valid @RequestBody NotificationTemplateRequest request
    ) {
        return ResponseEntity.status(201)
                .body(NotificationTemplateMapper.toResponse(
                        notificationTemplateService.createTemplate(
                                NotificationTemplateMapper.toEntity(request)
                        )
                ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationTemplateResponse> getTemplateById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                NotificationTemplateMapper.toResponse(
                        notificationTemplateService.getTemplateById(id)
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<NotificationTemplateResponse>> getTemplates(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) NotificationChannel channel
    ) {
        return ResponseEntity.ok(
                notificationTemplateService.getTemplates(name, channel)
                        .stream()
                        .map(NotificationTemplateMapper::toResponse)
                        .toList()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody NotificationTemplateRequest request
    ) {
        return ResponseEntity.ok(
                NotificationTemplateMapper.toResponse(
                        notificationTemplateService.updateTemplate(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        notificationTemplateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}