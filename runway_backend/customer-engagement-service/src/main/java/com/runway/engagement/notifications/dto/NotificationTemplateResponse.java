package com.runway.engagement.notifications.dto;

import com.runway.engagement.notifications.model.NotificationChannel;
import com.runway.engagement.notifications.model.NotificationEvent;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonPropertyOrder({"id", "event", "name", "channel", "subject", "content", "createdAt"})
public class NotificationTemplateResponse {

    private Long id;
    private NotificationEvent event;
    private String name;
    private String subject;
    private NotificationChannel channel; // can be EMAIL,SMS,PUSH
    private String content;
    private LocalDateTime createdAt;
}