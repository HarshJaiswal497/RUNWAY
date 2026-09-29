package com.runway.catalog.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        errorResponse.put("error", ex.getMessage());
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("path", "Resource not located");

        System.out.println("[Exception Handler] Caught ResourceNotFound: " + ex.getMessage() + " -> Sending Status: 404");

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(jakarta.validation.ConstraintViolationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", java.time.LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());

        // Extract the messages from the violations
        String errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(java.util.stream.Collectors.joining(", "));

        body.put("error", "Validation Failed");
        body.put("message", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("timestamp", java.time.LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");

        // Extract specific field errors: "price: Price must be non-negative"
        Map<String, String> fieldErrors = new java.util.HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        body.put("message", fieldErrors);
        body.put("path", "Check your request body");

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        String message = (ex.getMessage() != null) ? ex.getMessage() : "An unexpected error occurred";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // 1. Refined Status Selection
        if (message.toLowerCase().contains("unauthorized")) {
            status = HttpStatus.UNAUTHORIZED; // 401
        } else if (message.toLowerCase().contains("not found")) {
            status = HttpStatus.NOT_FOUND; // 404
        }

        // 2. Build Response Body
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        errorResponse.put("error", message);
        errorResponse.put("status", status.value());
        errorResponse.put("path", "Check your request URL"); // Optional: for debugging

        // 3. Log to console so you can see it's working
        System.out.println("[Exception Handler] Caught: " + message + " -> Sending Status: " + status);

        return new ResponseEntity<>(errorResponse, status);
    }
}