package com.runway.payment.exceptions;

/**
 * Thrown when a duplicate payment intent is attempted for an order that already has
 * an active (PENDING or SERVICE_UNAVAILABLE) payment.
 * Maps to HTTP 409 Conflict.
 */
public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(String message) {
        super(message);
    }

    public DuplicatePaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}

