package com.runway.engagement.notifications.service;

import com.runway.engagement.client.auth.AuthClient;
import com.runway.engagement.client.auth.dto.UserDetailsResponseDTO;
import com.runway.engagement.notifications.model.*;
import com.runway.engagement.notifications.repository.NotificationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationTemplateService templateService;
    private final NotificationLogRepository logRepository;
    private final AuthClient authClient;
    private final ObjectMapper objectMapper;

    public void notify(
            NotificationEvent event,
            Long userId,
            Map<String, Object> payload
    ) {
        log.info("Notification notify() called event={} userId={}", event, userId);

        NotificationTemplate template = templateService.getTemplateByEvent(event);

        // Serialize payload as JSON so it can be parsed back during reads
        String payloadJson = null;
        if (payload != null && !payload.isEmpty()) {
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (Exception e) {
                payloadJson = payload.toString(); // fallback
                log.warn("Could not serialize payload to JSON for event={}: {}", event, e.getMessage());
            }
        }

        // Save log with QUEUED status (channel forced to PUSH for in-app delivery)
        NotificationLog logEntity = new NotificationLog();
        logEntity.setUserId(userId);
        logEntity.setEvent(event);
        logEntity.setTemplate(template);
        logEntity.setChannel(NotificationChannel.PUSH);
        logEntity.setPayload(payloadJson);
        logEntity.setStatus(NotificationStatus.QUEUED);

        NotificationLog saved = logRepository.save(logEntity);

        // Fetch user details
        UserDetailsResponseDTO response = authClient.getUserDetails(userId);
        String email = response.getUser().getEmail();

        log.info("===== NOTIFICATION =====");
        log.info("UserId  : {}", userId);
        log.info("Email   : {}", email);
        log.info("Event   : {}", event);
        log.info("Subject : {}", template.getSubject());
        log.info("Payload : {}", payloadJson);
        log.info("========================");

        saved.setStatus(NotificationStatus.SENT);
        saved.setSentAt(LocalDateTime.now(ZoneOffset.UTC));
        logRepository.save(saved);

        log.info("Notification saved as SENT [event={}, userId={}, logId={}]", event, userId, saved.getId());
    }
}
