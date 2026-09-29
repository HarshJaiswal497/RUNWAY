-- =========================================================
-- V3: resolved columns + premium in-app notification templates
-- =========================================================

-- ── UPDATE EXISTING TEMPLATES ────────────────────────────

UPDATE notification_template SET
    name = 'ORDER_PLACED_INAPP', channel = 'PUSH',
    subject = 'Order confirmed — {{orderNumber}}',
    content = '{{amount}} · Being prepared for dispatch to {{shippingAddress}}.'
WHERE event = 'ORDER_PLACED';

UPDATE notification_template SET
    name = 'ORDER_CANCELLED_INAPP', channel = 'PUSH',
    subject = 'Order {{orderNumber}} cancelled',
    content = 'Your order has been cancelled. Any applicable refund will be processed within 5–7 business days.'
WHERE event = 'ORDER_CANCELLED';

UPDATE notification_template SET
    name = 'ORDER_DELIVERED_INAPP', channel = 'PUSH',
    subject = 'Your order has arrived',
    content = 'Order {{orderNumber}} delivered. We hope you love your pieces.'
WHERE event = 'ORDER_DELIVERED';

UPDATE notification_template SET
    name = 'SHIPMENT_DISPATCHED_INAPP', channel = 'PUSH',
    subject = 'Dispatched · {{trackingNumber}}',
    content = 'Order {{orderNumber}} is on its way with {{carrier}}.'
WHERE event = 'SHIPMENT_DISPATCHED';

UPDATE notification_template SET
    name = 'SHIPMENT_DELIVERED_INAPP', channel = 'PUSH',
    subject = 'Delivered — order {{orderNumber}}',
    content = 'Arrived on {{deliveredAt}}. Thank you for choosing RunWay.'
WHERE event = 'SHIPMENT_DELIVERED';

UPDATE notification_template SET
    name = 'REVIEW_SUBMITTED_INAPP', channel = 'PUSH',
    subject = 'Review received',
    content = 'Your review of {{productName}} is under moderation and will be published shortly.'
WHERE event = 'REVIEW_SUBMITTED';

UPDATE notification_template SET
    name = 'REVIEW_APPROVED_INAPP', channel = 'PUSH',
    subject = 'Your review is live — {{productName}}',
    content = 'Approved and now visible to other customers.'
WHERE event = 'REVIEW_APPROVED';

UPDATE notification_template SET
    name = 'REVIEW_REJECTED_INAPP', channel = 'PUSH',
    subject = 'Review not published',
    content = 'Your review of {{productName}} could not be published as it did not meet our community guidelines.'
WHERE event = 'REVIEW_REJECTED';

UPDATE notification_template SET
    name = 'RATING_SUBMITTED_INAPP', channel = 'PUSH',
    subject = '{{rating}} stars — {{productName}}',
    content = 'Your rating has been saved. Thank you for your feedback.'
WHERE event = 'RATING_SUBMITTED';

UPDATE notification_template SET
    name = 'TICKET_CREATED_INAPP', channel = 'PUSH',
    subject = 'Ticket #{{ticketId}} opened',
    content = '{{subject}} — Our team will respond within 24 hours.'
WHERE event = 'TICKET_CREATED';

UPDATE notification_template SET
    name = 'TICKET_RESOLVED_INAPP', channel = 'PUSH',
    subject = 'Ticket #{{ticketId}} resolved',
    content = '{{resolutionSummary}}'
WHERE event = 'TICKET_RESOLVED';

-- ── 3. INSERT MISSING EVENTS ──────────────────────────────

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'ORDER_CONFIRMED', 'ORDER_CONFIRMED_INAPP', 'PUSH',
       'Order {{orderNumber}} confirmed',
       'Confirmed and entering fulfilment. Expected dispatch within 1–2 business days.'
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE event = 'ORDER_CONFIRMED');

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'ORDER_SHIPPED', 'ORDER_SHIPPED_INAPP', 'PUSH',
       'Your order is on its way',
       'Order {{orderNumber}} has left our fulfilment centre and is headed to you.'
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE event = 'ORDER_SHIPPED');

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'PAYMENT_SUCCESS', 'PAYMENT_SUCCESS_INAPP', 'PUSH',
       'Payment confirmed — {{amount}}',
       'Order {{orderNumber}} · Payment received successfully.'
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE event = 'PAYMENT_SUCCESS');

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'PAYMENT_FAILED', 'PAYMENT_FAILED_INAPP', 'PUSH',
       'Payment unsuccessful',
       'We could not process {{amount}} for order {{orderNumber}}. Please update your payment details.'
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE event = 'PAYMENT_FAILED');

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'ACCOUNT_CREATED', 'ACCOUNT_CREATED_INAPP', 'PUSH',
       'Welcome to RunWay, {{userName}}',
       'Your account is ready. Discover our curated collections and exclusive designers.'
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE event = 'ACCOUNT_CREATED');
