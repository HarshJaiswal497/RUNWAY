package com.runway.engagement.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.use.kafka",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.use.kafka:false}")
    private boolean useKafka;

    @Override
    public void send(String subject, String content, Long userId, String email) {

        // Kafka OFF → LOG ONLY
        if (!useKafka) {
            log.info("===== NOTIFICATION (KAFKA OFF / LOG MODE) =====");
            log.info("UserId  : {}", userId);
            log.info("Email   : {}", email);
            log.info("Subject : {}", subject);
            log.info("Content : {}", content);
            log.info("=============================================");
            return;
        }

        // Kafka ON → REAL EMAIL SENDING
        log.info("Sending email notification [userId={}, email={}]", userId, email);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);

        log.info("Email sent successfully [userId={}, email={}]", userId, email);
    }
}