package com.runway.payment.service;

import com.runway.payment.model.PaymentAudit;
import com.runway.payment.repository.PaymentAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * PaymentAuditService — Handles all audit logging for payment transactions
 *
 * Responsibilities:
 * - Log payment creation
 * - Log status transitions
 * - Log failures and errors
 * - Maintain compliance audit trail
 */
@Service
public class PaymentAuditService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentAuditService.class);

    private final PaymentAuditRepository auditRepository;

    public PaymentAuditService(PaymentAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Log a payment action for audit trail
     */
    public void logAction(Long paymentIntentId, String action, String previousStatus, 
                         String newStatus, String details, Long userId) {
        try {
            PaymentAudit audit = new PaymentAudit(
                paymentIntentId,
                action,
                previousStatus,
                newStatus,
                details,
                userId
            );

            auditRepository.save(audit);
            logger.debug("Audit logged: Action={}, PaymentIntentId={}, Status={}->{}", 
                action, paymentIntentId, previousStatus, newStatus);
        } catch (Exception ex) {
            logger.error("Failed to log payment audit for intent {}", paymentIntentId, ex);
            // Don't throw exception - audit failure shouldn't break payment flow
        }
    }

    /**
     * Get audit history for a payment intent
     */
    public List<PaymentAudit> getAuditHistory(Long paymentIntentId) {
        try {
            return auditRepository.findByPaymentIntentIdOrderByCreatedAtDesc(paymentIntentId);
        } catch (Exception ex) {
            logger.error("Failed to retrieve audit history for intent {}", paymentIntentId, ex);
            return List.of();
        }
    }

    /**
     * Log payment creation
     */
    public void logPaymentCreated(Long paymentIntentId, Long orderId, Long userId) {
        logAction(paymentIntentId, "CREATED", null, "PENDING", 
            "Payment intent created for order " + orderId, userId);
    }

    /**
     * Log status transition
     */
    public void logStatusTransition(Long paymentIntentId, String previousStatus, 
                                   String newStatus, Long userId) {
        logAction(paymentIntentId, "STATUS_UPDATED", previousStatus, newStatus,
            "Status transitioned from " + previousStatus + " to " + newStatus, userId);
    }

    /**
     * Log payment failure
     */
    public void logPaymentFailed(Long paymentIntentId, String reason, Long userId) {
        logAction(paymentIntentId, "FAILED", null, "FAILED", 
            "Payment failed: " + reason, userId);
    }

    /**
     * Log shipment workflow trigger
     */
    public void logShipmentWorkflowTriggered(Long paymentIntentId, Long orderId, Long userId) {
        logAction(paymentIntentId, "SHIPMENT_TRIGGERED", null, "COMPLETED",
            "Shipment workflow triggered for order " + orderId, userId);
    }

    /**
     * Log shipment workflow failure
     */
    public void logShipmentWorkflowFailed(Long paymentIntentId, String reason, Long userId) {
        logAction(paymentIntentId, "SHIPMENT_FAILED", null, "COMPLETED",
            "Shipment workflow failed: " + reason, userId);
    }
}

