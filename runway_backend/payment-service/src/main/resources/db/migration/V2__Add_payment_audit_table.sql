-- Flyway Migration: Add payment_audit table for compliance and debugging
-- Version: V2__Add_payment_audit_table.sql

CREATE TABLE IF NOT EXISTS payment_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_intent_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50),
    details TEXT,
    user_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_audit_payment_intent (payment_intent_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_created (created_at),

    CONSTRAINT fk_audit_payment FOREIGN KEY (payment_intent_id)
        REFERENCES payment_intents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

