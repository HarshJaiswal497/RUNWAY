package com.runway.engagement.notifications.dto;

import com.runway.engagement.notifications.model.NotificationChannel;
import com.runway.engagement.notifications.model.NotificationEvent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class NotificationLogRequest {
    @NotNull(message = "User Id is required")
    private Long userId;

    @NotNull(message = "Notification event is required")
    private NotificationEvent event;

    private Map<String, Object> payload;
}