package com.runway.engagement.notifications.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PlaceholderResolver {

    public String resolve(String template, Map<String, Object> values) {
        String resolved = template;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            resolved = resolved.replace(
                    "{{" + entry.getKey() + "}}",
                    String.valueOf(entry.getValue())
            );
        }
        return resolved;
    }
}

