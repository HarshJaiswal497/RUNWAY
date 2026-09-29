package com.runway.payment.exceptions;

/**
 * Thrown when the payment amount is null or not positive.
 * Maps to HTTP 400 Bad Request.
 */
public class InvalidPaymentAmountException extends RuntimeException {

    public InvalidPaymentAmountException(String message) {
        super(message);
    }
}

