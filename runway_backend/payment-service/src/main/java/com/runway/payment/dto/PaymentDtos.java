package com.runway.payment.dto;

import com.runway.payment.model.PaymentIntent;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDtos {

    /** Request — create payment intent */
    public static class CreatePaymentIntentRequest {
        private BigDecimal amount;

        public CreatePaymentIntentRequest() {}

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    /** Request — update payment status */
    public static class UpdatePaymentStatusRequest {
        private String status;

        public UpdatePaymentStatusRequest() {}

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /** Response — payment intent (never expose raw entity) */
    public static class PaymentIntentResponse {
        private Long          id;
        private Long          orderId;
        private Long          userId;
        private BigDecimal    amount;
        private String        currency;
        private String        status;
        private String        razorpayOrderId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static PaymentIntentResponse from(PaymentIntent intent) {
            PaymentIntentResponse r = new PaymentIntentResponse();
            r.id              = intent.getId();
            r.orderId         = intent.getOrderId();
            r.userId          = intent.getUserId();
            r.amount          = intent.getAmount();
            r.currency        = intent.getCurrency();
            r.status          = intent.getStatus();
            r.razorpayOrderId = intent.getRazorpayOrderId();
            r.createdAt       = intent.getCreatedAt();
            r.updatedAt       = intent.getUpdatedAt();
            return r;
        }

        public Long getId() { return id; }
        public Long getOrderId() { return orderId; }
        public Long getUserId() { return userId; }
        public BigDecimal getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getStatus() { return status; }
        public String getRazorpayOrderId() { return razorpayOrderId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }

    /** Wrapper — consistent API response shape */
    public static class ApiResponse<T> {
        private boolean       success;
        private String        message;
        private T             data;
        private LocalDateTime timestamp;

        private ApiResponse() {}

        public static <T> ApiResponse<T> success(String message, T data) {
            ApiResponse<T> r = new ApiResponse<>();
            r.success   = true;
            r.message   = message;
            r.data      = data;
            r.timestamp = LocalDateTime.now();
            return r;
        }

        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> r = new ApiResponse<>();
            r.success   = false;
            r.message   = message;
            r.timestamp = LocalDateTime.now();
            return r;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public T getData() { return data; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}