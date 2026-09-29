package com.runway.payment.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central place to translate exceptions into HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 404 Not Found ────────────────────────────────────────────────────────
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotFound(PaymentNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ── 400 Bad Request ──────────────────────────────────────────────────────
    @ExceptionHandler(InvalidPaymentStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStatus(InvalidPaymentStatusException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidPaymentAmountException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidAmount(InvalidPaymentAmountException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ── 401 Unauthorized ─────────────────────────────────────────────────────
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // ── 409 Conflict (Duplicate Payment) ────────────────────────────────────
    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicatePayment(DuplicatePaymentException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ── 403 Forbidden ────────────────────────────────────────────────────────
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ── 503 Service Unavailable (Razorpay / external gateway) ────────────────
    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<Map<String, Object>> handleGatewayError(PaymentGatewayException ex) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    // ── 404 Static Resource Not Found (e.g., Swagger UI, actuator paths) ────
    // Must be listed BEFORE the catch-all Exception handler so Spring picks
    // the most-specific match first. Without this, NoResourceFoundException
    // falls through to handleGeneric() and becomes a misleading 500.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ── 500 Internal Server Error (catch-all) ────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage());
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

