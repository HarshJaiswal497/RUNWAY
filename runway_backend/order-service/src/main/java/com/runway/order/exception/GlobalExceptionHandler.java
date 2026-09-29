package com.runway.order.exception;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — Catches ALL exceptions from ALL controllers.
 * Returns consistent JSON error format for every error.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(build(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex, WebRequest request) {
        log.warn("Validation: {}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body(build(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field   = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        log.warn("Validation errors: {}", errors);
        return ResponseEntity.badRequest().body(
            ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now()).status(400)
                .error("Validation Failed")
                .message("Request validation failed")
                .path(path(request))
                .fieldErrors(errors).build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(build(HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action", request));
    }

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(
            feign.FeignException ex, WebRequest request) {
        log.error("Feign call failed [status={}]: {}", ex.status(), ex.getMessage());

        HttpStatus status;
        String message;

        if (ex.status() == 404) {
            status = HttpStatus.NOT_FOUND;
            message = "Requested resource not found in remote service.";
        } else if (ex.status() == 503 || ex.status() == -1) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Catalog Service is currently unavailable. Please try again later.";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Error communicating with remote service.";
        }

        return ResponseEntity.status(status)
                .body(build(status, message, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, WebRequest request) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again.", request));
    }

    private ErrorResponse build(HttpStatus status, String msg, WebRequest req) {
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now()).status(status.value())
            .error(status.getReasonPhrase()).message(msg)
            .path(path(req)).build();
    }

    private String path(WebRequest req) {
        return req.getDescription(false).replace("uri=", "");
    }

    @Data @Builder
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int           status;
        private String        error;
        private String        message;
        private String        path;
    }

    @Data @Builder
    public static class ValidationErrorResponse {
        private LocalDateTime       timestamp;
        private int                 status;
        private String              error;
        private String              message;
        private String              path;
        private Map<String, String> fieldErrors;
    }
}
