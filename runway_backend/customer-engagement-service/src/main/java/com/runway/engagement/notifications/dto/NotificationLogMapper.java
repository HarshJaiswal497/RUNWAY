package com.runway.engagement.notifications.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runway.engagement.notifications.model.NotificationLog;
import com.runway.engagement.notifications.model.NotificationTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NotificationLogMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static NotificationLog toEntity(
            NotificationLogRequest request,
            NotificationTemplate template
    ) {
        NotificationLog log = new NotificationLog();
        log.setUserId(request.getUserId());
        log.setTemplate(template);
        log.setChannel(template.getChannel());
        return log;
    }

    public static NotificationLogResponse toResponse(NotificationLog log) {
        NotificationLogResponse response = new NotificationLogResponse();
        response.setId(log.getId());
        response.setUserId(log.getUserId());
        response.setEvent(log.getEvent());
        response.setChannel(log.getChannel());
        response.setStatus(log.getStatus());
        response.setPayload(log.getPayload());
        response.setErrorMessage(log.getErrorMessage());
        response.setCreatedAt(log.getCreatedAt());
        response.setSentAt(log.getSentAt());
        response.setReadAt(log.getReadAt());

        // Resolve {{placeholders}} at read time using stored payload.
        // Works for both old records (Java Map.toString format) and new records (JSON).
        Map<String, String> vars = parsePayload(log.getPayload());
        NotificationTemplate template = log.getTemplate();
        response.setSubject(template != null ? resolve(template.getSubject(), vars) : null);
        response.setContent(template != null ? resolve(template.getContent(), vars) : null);

        return response;
    }

    private static Map<String, String> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) return Collections.emptyMap();

        // Try JSON first (new records)
        if (payload.startsWith("{") && payload.contains(":")) {
            try {
                Map<String, Object> raw = OBJECT_MAPPER.readValue(
                        payload, new TypeReference<Map<String, Object>>() {});
                Map<String, String> result = new HashMap<>();
                raw.forEach((k, v) -> result.put(k, v != null ? String.valueOf(v) : ""));
                return result;
            } catch (Exception ignored) {}
        }

        // Fall back to Java Map.toString() format: {key=value, key2=value2}
        Map<String, String> result = new HashMap<>();
        if (!payload.startsWith("{") || payload.length() < 3) return result;
        String inner = payload.substring(1, payload.length() - 1).trim();
        if (inner.isEmpty()) return result;
        // Split on ", " only when followed by "word=" (avoids splitting on commas inside values)
        for (String pair : inner.split(",\\s*(?=[\\w]+=)")) {
            int eq = pair.indexOf('=');
            if (eq > 0) result.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
        }
        return result;
    }

    private static String resolve(String template, Map<String, String> vars) {
        if (template == null) return null;
        String result = template;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            result = result.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        return result;
    }
}
