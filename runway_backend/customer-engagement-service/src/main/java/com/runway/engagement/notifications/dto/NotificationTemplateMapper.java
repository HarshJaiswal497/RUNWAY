package com.runway.engagement.notifications.dto;

import com.runway.engagement.notifications.model.NotificationTemplate;

public class NotificationTemplateMapper {

    public static NotificationTemplate toEntity(NotificationTemplateRequest request) {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setEvent(request.getEvent());
        notificationTemplate.setName(request.getName());
        notificationTemplate.setChannel(request.getChannel());
        notificationTemplate.setSubject(request.getSubject());
        notificationTemplate.setContent(request.getContent());
        return notificationTemplate;
    }

    public static NotificationTemplateResponse toResponse(NotificationTemplate notificationTemplate) {
        NotificationTemplateResponse response = new NotificationTemplateResponse();
        response.setId(notificationTemplate.getId());
        response.setEvent(notificationTemplate.getEvent());
        response.setName(notificationTemplate.getName());
        response.setChannel(notificationTemplate.getChannel());
        response.setSubject(notificationTemplate.getSubject());
        response.setContent(notificationTemplate.getContent());
        response.setCreatedAt(notificationTemplate.getCreatedAt());
        return response;
    }
}
