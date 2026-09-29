package com.runway.payment.controller;

import com.runway.payment.dto.PaymentDtos.ApiResponse;
import com.runway.payment.dto.PaymentDtos.CreatePaymentIntentRequest;
import com.runway.payment.dto.PaymentDtos.PaymentIntentResponse;
import com.runway.payment.dto.PaymentDtos.UpdatePaymentStatusRequest;
import com.runway.payment.exceptions.InvalidPaymentAmountException;
import com.runway.payment.exceptions.UnauthorizedException;
import com.runway.payment.model.PaymentIntent;
import com.runway.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{orderId}/intent")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> createIntent(
            @PathVariable Long orderId,
            @RequestBody CreatePaymentIntentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long gatewayUserId) {

        if (gatewayUserId == null) {
            throw new UnauthorizedException(
                    "Unauthorized: Missing X-User-Id header. Use API Gateway.");
        }

        if (request.getAmount() == null) {
            throw new InvalidPaymentAmountException(
                    "Amount field is required in request body.");
        }

        log.debug("POST /payment/{}/intent - userId={}, amount={}",
                orderId, gatewayUserId, request.getAmount());

        PaymentIntent intent = paymentService.createPaymentIntent(
                orderId, request.getAmount(), gatewayUserId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Payment intent created successfully",
                        PaymentIntentResponse.from(intent)));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> updateStatus(
            @PathVariable Long orderId,
            @RequestBody UpdatePaymentStatusRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long gatewayUserId,
            @RequestHeader(value = "X-User-Role", required = false) String gatewayUserRole) {

        if (gatewayUserId == null
                || gatewayUserRole == null
                || gatewayUserRole.isBlank()) {
            throw new UnauthorizedException(
                    "Unauthorized: Missing gateway authentication headers.");
        }

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("The 'status' field is required."));
        }

        log.debug("PUT /payment/{}/status - status={} by userId={}",
                orderId, request.getStatus(), gatewayUserId);

        PaymentIntent updatedIntent = paymentService.updatePaymentStatus(
                orderId, request.getStatus(), gatewayUserId, gatewayUserRole);

        return ResponseEntity.ok(ApiResponse.success(
                "Payment status updated to " + updatedIntent.getStatus(),
                PaymentIntentResponse.from(updatedIntent)));
    }
    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> confirmPayment(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            throw new UnauthorizedException("Missing X-User-Id header.");
        }

        PaymentIntent intent = paymentService.updatePaymentStatus(
                orderId, "COMPLETED", userId, "ADMIN");

        return ResponseEntity.ok(ApiResponse.success(
                "Payment confirmed successfully",
                PaymentIntentResponse.from(intent)));
    }
}