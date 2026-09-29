package com.runway.engagement.notifications.dto;

import com.runway.engagement.notifications.model.NotificationChannel;
import com.runway.engagement.notifications.model.NotificationEvent;
import com.runway.engagement.notifications.model.NotificationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonPropertyOrder({
        "id",
        "userId",
        "event",
        "channel",
        "status",
        "subject",
        "content",
        "payload",
        "errorMessage",
        "createdAt",
        "sentAt",
        "readAt",
        "read"
})
public class NotificationLogResponse {

    private Long id;
    private Long userId;
    private NotificationEvent event;
    private NotificationChannel channel;
    private NotificationStatus status;
    private String subject;
    private String content;
    private String payload;
    private String errorMessage;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime sentAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime readAt;

    public boolean isRead() {
        return readAt != null;
    }
}
