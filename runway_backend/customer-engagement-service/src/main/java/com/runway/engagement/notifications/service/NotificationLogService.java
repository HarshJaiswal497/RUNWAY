package com.runway.engagement.notifications.service;

import com.runway.engagement.notifications.exception.NotifcationLogNotFound;
import com.runway.engagement.notifications.model.NotificationLog;
import com.runway.engagement.notifications.model.NotificationStatus;
import com.runway.engagement.notifications.repository.NotificationLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class NotificationLogService {

    private final NotificationLogRepository repository;

    public NotificationLogService(NotificationLogRepository repository) {
        this.repository = repository;
    }

    //  fetch logs (mine / status)
    public List<NotificationLog> getLogs(
            boolean mine,
            NotificationStatus status,
            Long userId,
            boolean isCustomer
    ) {
        // CUSTOMER: always scoped to own logs
        if (isCustomer) {
            if (status != null) {
                return repository.findByUserIdAndStatus(userId, status);
            }
            return repository.findByUserId(userId);
        }

        // ADMIN / SUPPORT
        if (status != null) {
            return repository.findByStatus(status);
        }

        return repository.findAll();
    }

    //  get by id
    public NotificationLog getLogById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new NotifcationLogNotFound("No such notification log found")
                );
    }

    public void markRead(Long id, Long callerId) {
        NotificationLog log = repository.findById(id)
                .orElseThrow(() -> new NotifcationLogNotFound("No such notification log found"));
        // only the owner can mark their own notification as read
        if (log.getUserId() != callerId) return;
        if (log.getReadAt() == null) {
            log.setReadAt(LocalDateTime.now(ZoneOffset.UTC));
            repository.save(log);
        }
    }

    public void markAllRead(Long userId) {
        List<NotificationLog> unread = repository.findByUserIdAndReadAtIsNull(userId);
        if (unread.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        unread.forEach(log -> log.setReadAt(now));
        repository.saveAll(unread);
    }
}
