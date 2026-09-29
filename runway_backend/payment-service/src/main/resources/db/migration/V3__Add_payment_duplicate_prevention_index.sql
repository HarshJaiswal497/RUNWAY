-- Flyway Migration: Add index for duplicate payment prevention
-- Version: V3__Add_payment_duplicate_prevention_index.sql

-- Add index for efficient querying of PENDING/SERVICE_UNAVAILABLE payments
ALTER TABLE payment_intents
ADD INDEX idx_order_status (order_id, status);

