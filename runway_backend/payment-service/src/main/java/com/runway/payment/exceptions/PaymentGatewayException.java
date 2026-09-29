package com.runway.payment.exceptions;

/**
 * Thrown when the Razorpay (or any external payment gateway) call fails.
 * Maps to HTTP 503 Service Unavailable.
 */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}

