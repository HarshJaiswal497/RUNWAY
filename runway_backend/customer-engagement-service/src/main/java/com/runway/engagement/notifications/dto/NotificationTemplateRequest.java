package com.runway.engagement.notifications.dto;

import com.runway.engagement.notifications.model.NotificationChannel;
import com.runway.engagement.notifications.model.NotificationEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationTemplateRequest {

    @NotBlank(message = "Name must be given to the template")
    private String name;

    @NotNull(message = "Please provide notification channel")
    private NotificationChannel channel; // can be EMAIL,SMS,PUSH

    @NotNull(message = "Please provide notification event")
    private NotificationEvent event;

    @NotBlank(message = "Content should not be empty")
    private String content;

    private String subject;
}