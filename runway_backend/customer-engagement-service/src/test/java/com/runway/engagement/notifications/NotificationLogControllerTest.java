package com.runway.engagement.notifications;

import com.runway.engagement.notifications.controller.NotificationLogController;
import com.runway.engagement.notifications.dto.NotificationLogRequest;
import com.runway.engagement.notifications.model.*;
import com.runway.engagement.notifications.service.NotificationLogService;
import com.runway.engagement.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationLogControllerTest {

    @Mock
    private NotificationLogService notificationLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationLogController controller;

    // -------------------------
    // POST /notification-logs
    // -------------------------
    @Test
    void logNotification_returns201() {
        NotificationLogRequest request = new NotificationLogRequest();
        request.setEvent(NotificationEvent.RATING_SUBMITTED);
        request.setUserId(10L);
        request.setPayload(Map.of("rating", 5));

        doNothing().when(notificationService).notify(
                NotificationEvent.RATING_SUBMITTED,
                10L,
                request.getPayload()
        );

        var response = controller.logNotification(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        verify(notificationService).notify(
                NotificationEvent.RATING_SUBMITTED,
                10L,
                request.getPayload()
        );
    }

    // --------------------------------
// GET /notification-logs (mine=true)
// --------------------------------
    @Test
    void getLogs_mineTrue_returnsCustomerLogs() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(10L);


        Collection<GrantedAuthority> authorities =
                List.of((GrantedAuthority) () -> "ROLE_CUSTOMER");

        doReturn(authorities).when(auth).getAuthorities();

        NotificationLog log = new NotificationLog();
        log.setId(1L);
        log.setUserId(10L);
        log.setStatus(NotificationStatus.SENT);

        when(notificationLogService.getLogs(true, null, 10L, true))
                .thenReturn(List.of(log));

        var response = controller.getLogs(true, null, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        verify(notificationLogService)
                .getLogs(true, null, 10L, true);
    }

    // -----------------------------------------
    // GET /notification-logs (status filter)
    // -----------------------------------------
    @Test
    void getLogs_statusFilter_returnsFilteredLogs() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(99L);
        doAnswer(invocation -> List.of((GrantedAuthority) () -> "ROLE_ADMIN"))
                .when(auth)
                .getAuthorities();

        NotificationLog log = new NotificationLog();
        log.setId(2L);
        log.setStatus(NotificationStatus.FAILED);

        when(notificationLogService.getLogs(false, NotificationStatus.FAILED, 99L, false))
                .thenReturn(List.of(log));

        var response = controller.getLogs(false, NotificationStatus.FAILED, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        verify(notificationLogService)
                .getLogs(false, NotificationStatus.FAILED, 99L, false);
    }

    // --------------------------------
    // GET /notification-logs/{id}
    // --------------------------------
    @Test
    void getLogById_returnsLog() {
        NotificationLog log = new NotificationLog();
        log.setId(42L);
        log.setUserId(10L);
        log.setStatus(NotificationStatus.SENT);

        when(notificationLogService.getLogById(42L))
                .thenReturn(log);

        var response = controller.getLogById(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        verify(notificationLogService).getLogById(42L);
    }
}