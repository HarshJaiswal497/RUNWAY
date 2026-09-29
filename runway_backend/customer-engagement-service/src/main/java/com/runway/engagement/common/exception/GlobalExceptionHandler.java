package com.runway.engagement.common.exception;

import com.runway.engagement.notifications.exception.NotificationTemplateNotFoundException;
import com.runway.engagement.reviews.exception.ReviewNotFoundException;
import com.runway.engagement.support.exception.TicketNotFoundException;
import com.runway.engagement.notifications.exception.NotifcationLogNotFound;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse();
        response.setTimestamp(LocalDateTime.now());
        response.setStatus(status.value());
        response.setError(status.getReasonPhrase());
        response.setMessage(ex.getMessage());
        response.setPath(request.getRequestURI());
        return ResponseEntity.status(status).body(response);
    }
    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReviewNotFound(ReviewNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketNotFound(TicketNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(NotifcationLogNotFound.class)
    public ResponseEntity<ErrorResponse> handleNotificationLogNotFound(NotifcationLogNotFound ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(NotificationTemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotificationTemplateNotFound(NotificationTemplateNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse();
        response.setTimestamp(LocalDateTime.now());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setError(HttpStatus.FORBIDDEN.getReasonPhrase());
        response.setMessage(ex.getMessage() != null ? ex.getMessage() : "Access denied");
        response.setPath(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }


    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

}
