package com.runway.engagement.notifications.service;


import com.runway.engagement.notifications.model.NotificationChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(
        name = "app.use.kafka",
        havingValue = "true",
        matchIfMissing = false
)
public class NotificationSenderFactory {

    private final EmailNotificationSender emailSender;

    public NotificationSenderFactory(EmailNotificationSender emailSender) {
        this.emailSender = emailSender;
    }

    public NotificationSender getSender(NotificationChannel channel) {
        return emailSender; // always email for now
    }
}