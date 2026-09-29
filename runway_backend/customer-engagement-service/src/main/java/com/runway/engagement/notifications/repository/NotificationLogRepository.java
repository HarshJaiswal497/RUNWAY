package com.runway.engagement.notifications.repository;

import com.runway.engagement.notifications.model.NotificationLog;
import com.runway.engagement.notifications.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUserId(Long userId);
    List<NotificationLog> findByUserIdAndStatus(Long userId, NotificationStatus status);
    List<NotificationLog> findByStatus(NotificationStatus status);
    List<NotificationLog> findByUserIdAndReadAtIsNull(Long userId);
}
