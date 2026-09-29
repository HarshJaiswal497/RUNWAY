package com.runway.engagement.notifications;

import com.runway.engagement.notifications.exception.NotifcationLogNotFound;
import com.runway.engagement.notifications.model.NotificationLog;
import com.runway.engagement.notifications.model.NotificationStatus;
import com.runway.engagement.notifications.repository.NotificationLogRepository;
import com.runway.engagement.notifications.service.NotificationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationLogServiceTest {

    @Mock
    private NotificationLogRepository repository;

    @InjectMocks
    private NotificationLogService service;

    // -------------------------
    // getLogs - CUSTOMER (no status)
    // -------------------------
    @Test
    void getLogs_customerWithoutStatus_returnsOwnLogs() {
        NotificationLog log = new NotificationLog();
        log.setUserId(10L);

        when(repository.findByUserId(10L))
                .thenReturn(List.of(log));

        List<NotificationLog> result =
                service.getLogs(true, null, 10L, true);

        assertThat(result).hasSize(1);
        verify(repository).findByUserId(10L);
        verifyNoMoreInteractions(repository);
    }

    // -------------------------
    // getLogs - CUSTOMER (with status)
    // -------------------------
    @Test
    void getLogs_customerWithStatus_returnsFilteredLogs() {
        NotificationLog log = new NotificationLog();
        log.setUserId(10L);
        log.setStatus(NotificationStatus.SENT);

        when(repository.findByUserIdAndStatus(10L, NotificationStatus.SENT))
                .thenReturn(List.of(log));

        List<NotificationLog> result =
                service.getLogs(true, NotificationStatus.SENT, 10L, true);

        assertThat(result).hasSize(1);
        verify(repository)
                .findByUserIdAndStatus(10L, NotificationStatus.SENT);
        verifyNoMoreInteractions(repository);
    }

    // -------------------------
    // getLogs - ADMIN (no status)
    // -------------------------
    @Test
    void getLogs_adminWithoutStatus_returnsAllLogs() {
        NotificationLog log = new NotificationLog();

        when(repository.findAll())
                .thenReturn(List.of(log));

        List<NotificationLog> result =
                service.getLogs(false, null, 99L, false);

        assertThat(result).hasSize(1);
        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    // -------------------------
    // getLogs - ADMIN (with status)
    // -------------------------
    @Test
    void getLogs_adminWithStatus_returnsFilteredLogs() {
        NotificationLog log = new NotificationLog();
        log.setStatus(NotificationStatus.FAILED);

        when(repository.findByStatus(NotificationStatus.FAILED))
                .thenReturn(List.of(log));

        List<NotificationLog> result =
                service.getLogs(false, NotificationStatus.FAILED, 99L, false);

        assertThat(result).hasSize(1);
        verify(repository).findByStatus(NotificationStatus.FAILED);
        verifyNoMoreInteractions(repository);
    }

    // -------------------------
    // getLogById - success
    // -------------------------
    @Test
    void getLogById_whenFound_returnsLog() {
        NotificationLog log = new NotificationLog();
        log.setId(42L);

        when(repository.findById(42L))
                .thenReturn(Optional.of(log));

        NotificationLog result = service.getLogById(42L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(42L);
        verify(repository).findById(42L);
    }

    // -------------------------
    // getLogById - not found
    // -------------------------
    @Test
    void getLogById_whenNotFound_throwsException() {
        when(repository.findById(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLogById(100L))
                .isInstanceOf(NotifcationLogNotFound.class)
                .hasMessage("No such notification log found");

        verify(repository).findById(100L);
    }
}