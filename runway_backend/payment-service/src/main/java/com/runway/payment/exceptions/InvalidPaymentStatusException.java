package com.runway.payment.exceptions;

/**
 * Thrown when a caller provides a payment status value that is not allowed.
 * Maps to HTTP 400 Bad Request.
 */
public class InvalidPaymentStatusException extends RuntimeException {

    public InvalidPaymentStatusException(String status) {
        super("Invalid payment status: '" + status + "'. Allowed values are COMPLETED or FAILED.");
    }
}

