package com.runway.payment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * PaymentAudit — JPA Entity for compliance and debugging
 *
 * Tracks all payment-related actions for audit trail:
 * - Payment creation
 * - Status transitions
 * - Failures
 * - User actions
 *
 * Essential for:
 * - PCI-DSS compliance
 * - Fraud detection
 * - Payment reconciliation
 * - Debugging payment issues
 */
@Entity
@Table(name = "payment_audit",
    indexes = {
        @Index(name = "idx_audit_payment_intent", columnList = "payment_intent_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_created", columnList = "created_at")
    }
)
public class PaymentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_intent_id", nullable = false)
    private Long paymentIntentId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;  // CREATED, STATUS_UPDATED, FAILED, COMPLETED, WEBHOOK_RECEIVED

    @Column(name = "previous_status", length = 50)
    private String previousStatus;

    @Column(name = "new_status", length = 50)
    private String newStatus;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PaymentAudit() {
    }

    public PaymentAudit(Long paymentIntentId, String action, String previousStatus, 
                       String newStatus, String details, Long userId) {
        this.paymentIntentId = paymentIntentId;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.details = details;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(Long paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

