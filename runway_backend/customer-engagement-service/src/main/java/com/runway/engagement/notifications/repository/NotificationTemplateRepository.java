package com.runway.engagement.notifications.repository;

import com.runway.engagement.notifications.model.NotificationChannel;
import com.runway.engagement.notifications.model.NotificationEvent;
import com.runway.engagement.notifications.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByName(String name);

    List<NotificationTemplate> findByChannel(NotificationChannel channel);

    Optional<NotificationTemplate> findByEvent(NotificationEvent event);
}
