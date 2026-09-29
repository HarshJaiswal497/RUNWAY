package com.runway.engagement.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InternalNotifyRequest {
    private String event;
    private Long userId;
    private Map<String, Object> payload;
}
