package com.runway.engagement.notifications.service;

public interface NotificationSender {
    void send(String subject, String content, Long userId, String email);
}