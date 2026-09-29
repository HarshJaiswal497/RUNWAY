package com.runway.payment.exceptions;

/**
 * Thrown when no PaymentIntent is found for the given order ID.
 * Maps to HTTP 404 Not Found.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long orderId) {
        super("Payment Intent not found for order ID: " + orderId);
    }
}

