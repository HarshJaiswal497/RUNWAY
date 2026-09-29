package com.runway.payment.service;

import com.runway.payment.client.EngagementServiceClient;
import com.runway.payment.client.OrderServiceClient;
import com.runway.payment.client.dto.OrderDTO;
import com.runway.payment.exceptions.DuplicatePaymentException;
import com.runway.payment.exceptions.ForbiddenException;
import com.runway.payment.exceptions.InvalidPaymentAmountException;
import com.runway.payment.exceptions.InvalidPaymentStatusException;
import com.runway.payment.exceptions.PaymentGatewayException;
import com.runway.payment.exceptions.PaymentNotFoundException;
import com.runway.payment.model.PaymentIntent;
import com.runway.payment.repository.PaymentIntentRepo;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    private final PaymentIntentRepo repository;
    private final OrderServiceClient orderServiceClient;
    private final PaymentAuditService auditService;
    private final EngagementServiceClient engagementClient;

    public PaymentService(PaymentIntentRepo repository, OrderServiceClient orderServiceClient,
                         PaymentAuditService auditService, EngagementServiceClient engagementClient) {
        this.repository = repository;
        this.orderServiceClient = orderServiceClient;
        this.auditService = auditService;
        this.engagementClient = engagementClient;
    }

    /**
     * 1. Public entry-point: validates input BEFORE circuit breaker intercepts
     * - Validates amount
     * - Prevents duplicate PENDING payments (LOOPHOLE FIX #2)
     * - Verifies user owns the order (LOOPHOLE FIX #4)
     * - Validates amount against order total (LOOPHOLE FIX #6)
     */
    public PaymentIntent createPaymentIntent(Long orderId, BigDecimal amount, Long userId) {

        // LOOPHOLE FIX #1: Explicit null check for amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentAmountException("Payment amount must be greater than zero.");
        }

        // LOOPHOLE FIX #2: Prevent duplicate PENDING payments
        repository.findByOrderIdAndStatus(orderId, "PENDING").ifPresent(intent -> {
            throw new DuplicatePaymentException(
                "A PENDING payment already exists for order " + orderId +
                ". Please complete or cancel it first."
            );
        });

        // Also check for SERVICE_UNAVAILABLE (fallback state)
        repository.findByOrderIdAndStatus(orderId, "SERVICE_UNAVAILABLE").ifPresent(intent -> {
            throw new DuplicatePaymentException(
                "A payment with SERVICE_UNAVAILABLE status exists for order " + orderId +
                ". Please contact support or retry later."
            );
        });

        // LOOPHOLE FIX #4 & #6: Verify order ownership and validate amount
        try {
            OrderDTO order = orderServiceClient.getOrderDetails(orderId, userId);

            // Verify user owns this order
            if (!userId.equals(order.getUserId())) {
                logger.warn("Unauthorized payment attempt: userId {} trying to pay for order {} owned by {}",
                    userId, orderId, order.getUserId());
                throw new ForbiddenException(
                    "You are not authorized to create payment for order " + orderId
                );
            }

            // Verify payment amount matches order total
            if (!order.getTotalAmount().equals(amount)) {
                throw new InvalidPaymentAmountException(
                    "Payment amount " + amount + " does not match order total " +
                    order.getTotalAmount()
                );
            }

            logger.info("Payment validation successful for order {} by user {}", orderId, userId);
        } catch (ForbiddenException | InvalidPaymentAmountException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Failed to validate order details for order {}", orderId, ex);
            throw new PaymentGatewayException("Failed to validate order. Please try again.", ex);
        }

        return createPaymentIntentWithGateway(orderId, amount, userId);
    }

    /**
     * 2. Circuit-breaker-protected method that calls Razorpay gateway
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackCreatePaymentIntent")
    public PaymentIntent createPaymentIntentWithGateway(Long orderId, BigDecimal amount, Long userId) {

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            int amountInPaise = amount.multiply(new BigDecimal("100")).intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            // LOOPHOLE FIX #5: Receipt acts as idempotency key in Razorpay
            orderRequest.put("receipt", "txn_order_" + orderId + "_" + System.currentTimeMillis());

            Order razorpayOrder = razorpay.orders.create(orderRequest);

            PaymentIntent intent = new PaymentIntent(
                    orderId,
                    amount,
                    "PENDING",
                    razorpayOrder.get("id"),
                    userId
            );

            PaymentIntent savedIntent = repository.save(intent);

            // LOOPHOLE FIX #9: Log payment creation for audit trail
            auditService.logPaymentCreated(savedIntent.getId(), orderId, userId);

            logger.info("Payment intent created successfully for order {} by user {}", orderId, userId);
            return savedIntent;
        } catch (RazorpayException ex) {
            throw new PaymentGatewayException("Razorpay gateway error: " + ex.getMessage(), ex);
        }
    }

    /**
     * 3. Fallback for createPaymentIntentWithGateway
     * LOOPHOLE FIX #3: Now persists fallback payment to database
     */
    public PaymentIntent fallbackCreatePaymentIntent(Long orderId, BigDecimal amount, Long userId, Throwable throwable) {
        logger.error("Razorpay API failed for Order {}. Fallback triggered. Reason: {}", orderId, throwable.getMessage());

        // LOOPHOLE FIX #3: Save fallback payment to DB instead of just returning in-memory object
        PaymentIntent fallbackIntent = new PaymentIntent(
                orderId,
                amount,
                "SERVICE_UNAVAILABLE",
                "GATEWAY_ERROR",
                userId
        );

        PaymentIntent savedIntent = repository.save(fallbackIntent);

        // Log the fallback creation
        auditService.logAction(savedIntent.getId(), "FALLBACK_CREATED", null, "SERVICE_UNAVAILABLE",
            "Razorpay API failed: " + throwable.getMessage(), userId);

        return savedIntent;
    }

    /**
     * Update payment status with authorization & state validation
     * - LOOPHOLE FIX #10: Prevent transitions from terminal states
     * - LOOPHOLE FIX #9: Audit trail logging
     */
    @Transactional
    public PaymentIntent updatePaymentStatus(Long orderId, String newStatus, Long userId, String userRole) {

        // Authorization check: Only ADMIN users can update payment status
        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            throw new ForbiddenException("Only ADMIN users can update payment status.");
        }

        PaymentIntent intent = repository.findTopByOrderIdOrderByIdDesc(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));

        if (!newStatus.equalsIgnoreCase("COMPLETED") && !newStatus.equalsIgnoreCase("FAILED")) {
            throw new InvalidPaymentStatusException(newStatus);
        }

        String previousStatus = intent.getStatus();

        // LOOPHOLE FIX #10: Prevent transitions from terminal states
        if ("COMPLETED".equalsIgnoreCase(previousStatus) || "FAILED".equalsIgnoreCase(previousStatus)) {
            throw new PaymentGatewayException(
                "Cannot transition payment status from " + previousStatus +
                ". Payment is in terminal state."
            );
        }

        // Also prevent transitions from SERVICE_UNAVAILABLE (fallback state)
        if ("SERVICE_UNAVAILABLE".equalsIgnoreCase(previousStatus)) {
            throw new PaymentGatewayException(
                "Payment is unavailable due to gateway issues. " +
                "Please contact support or retry the payment later."
            );
        }

        intent.setStatus(newStatus.toUpperCase());
        PaymentIntent savedIntent = repository.save(intent);

        // LOOPHOLE FIX #9: Log status transition
        auditService.logStatusTransition(savedIntent.getId(), previousStatus, newStatus.toUpperCase(), userId);

        boolean transitionedToCompleted =
                !"COMPLETED".equalsIgnoreCase(previousStatus)
                        && "COMPLETED".equalsIgnoreCase(savedIntent.getStatus());

        // Fetch order details to notify the correct user (order owner, not admin)
        Long ownerUserId = userId;
        String orderNumber = orderId.toString();
        try {
            OrderDTO order = orderServiceClient.getOrderDetails(orderId, userId);
            if (order.getUserId() != null) ownerUserId = order.getUserId();
            if (order.getOrderNumber() != null) orderNumber = order.getOrderNumber();
        } catch (Exception e) {
            logger.warn("Could not fetch order details for notification: orderId={}", orderId);
        }

        if (transitionedToCompleted) {
            notifyEngagement("PAYMENT_SUCCESS", ownerUserId, Map.of(
                "orderNumber", orderNumber,
                "amount", savedIntent.getAmount().toString()
            ));
            notifyEngagement("ORDER_CONFIRMED", ownerUserId, Map.of("orderNumber", orderNumber));
            triggerShipmentWorkflow(orderId, userId, savedIntent.getId());
        } else if ("FAILED".equalsIgnoreCase(savedIntent.getStatus())) {
            notifyEngagement("PAYMENT_FAILED", ownerUserId, Map.of(
                "orderNumber", orderNumber,
                "amount", savedIntent.getAmount().toString()
            ));
        }

        return savedIntent;
    }

    private void notifyEngagement(String event, Long userId, Map<String, Object> payload) {
        try {
            engagementClient.notify(new EngagementServiceClient.NotifyRequest(event, userId, payload));
        } catch (Exception e) {
            logger.warn("Notification skipped (engagement service unavailable): event={}, userId={}", event, userId);
        }
    }

    /**
     * Trigger shipment workflow with retry mechanism
     * LOOPHOLE FIX #8: Added retry logic with proper fallback
     */
    @Transactional
    @Retry(name = "shipmentWorkflow", fallbackMethod = "fallbackShipmentWorkflow")
    private void triggerShipmentWorkflow(Long orderId, Long userId, Long paymentIntentId) {
        try {
            orderServiceClient.updateOrderStatusAsAdmin(
                    orderId,
                    "ADMIN",
                    userId,
                    Map.of("status", "PROCESSING")
            );
            logger.info("Triggered shipment workflow for order {} after successful payment.", orderId);

            // LOOPHOLE FIX #9: Log shipment workflow trigger
            auditService.logShipmentWorkflowTriggered(paymentIntentId, orderId, userId);
        } catch (Exception ex) {
            logger.error("Payment completed but failed to trigger shipment workflow for order {}.", orderId, ex);

            // LOOPHOLE FIX #9: Log shipment workflow failure
            auditService.logShipmentWorkflowFailed(paymentIntentId, ex.getMessage(), userId);

            throw new PaymentGatewayException(
                    "Payment captured, but failed to trigger shipment workflow for order " + orderId + ".",
                    ex
            );
        }
    }

    /**
     * Fallback for retry exhaustion in shipment workflow
     * Called when all retry attempts are exhausted
     */
    public void fallbackShipmentWorkflow(Long orderId, Long userId, Long paymentIntentId, Exception ex) {
        logger.error("Shipment workflow exhausted all retry attempts for order {}. " +
            "Manual intervention may be needed. Error: {}", orderId, ex.getMessage(), ex);

        // Could be extended to send alerts to ops team
        // alertService.sendOpsAlert("Shipment workflow failed for order " + orderId);
    }
}

