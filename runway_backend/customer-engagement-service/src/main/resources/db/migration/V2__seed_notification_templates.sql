-- =========================================================
-- SEED NOTIFICATION TEMPLATES (IDEMPOTENT)
-- =========================================================

-- ===============================
-- ORDER EVENTS
-- ===============================

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'ORDER_PLACED', 'ORDER_PLACED_EMAIL', 'EMAIL',
       'Your order has been placed successfully!',
       'Hi {{userName}},\n\nYour order {{orderNumber}} has been placed successfully.\n\nTotal Amount: {{amount}}\nShipping Address: {{shippingAddress}}\n\nThank you for shopping with us!'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template WHERE event = 'ORDER_PLACED'
);

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'ORDER_CANCELLED', 'ORDER_CANCELLED_EMAIL', 'EMAIL',
       'Your order has been cancelled',
       'Your order {{orderNumber}} has been cancelled.\n\nIf any refund is applicable, it will be processed shortly.'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template WHERE event = 'ORDER_CANCELLED'
);

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'ORDER_DELIVERED', 'ORDER_DELIVERED_PUSH', 'PUSH',
       NULL,
       'Your order {{orderNumber}} has been delivered successfully!'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template WHERE event = 'ORDER_DELIVERED'
);

-- ===============================
-- SHIPMENT EVENTS
-- ===============================

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'SHIPMENT_DISPATCHED', 'SHIPMENT_DISPATCHED_SMS', 'SMS',
       NULL,
       'Your order {{orderNumber}} has been dispatched.\nTracking ID: {{trackingNumber}}\nCarrier: {{carrier}}'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template WHERE event = 'SHIPMENT_DISPATCHED'
);

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'SHIPMENT_DELIVERED', 'SHIPMENT_DELIVERED_EMAIL', 'EMAIL',
       'Shipment delivered',
       'Your shipment for order {{orderNumber}} was delivered on {{deliveredAt}}.'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template WHERE event = 'SHIPMENT_DELIVERED'
);

-- ===============================
-- REVIEW EVENTS
-- ===============================

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'REVIEW_SUBMITTED', 'REVIEW_SUBMITTED_EMAIL', 'EMAIL',
       'Your review is under moderation',
       'Thank you for reviewing {{productName}}.\n\nYour review is currently under moderation and will be published once approved.'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template WHERE event = 'REVIEW_SUBMITTED'
);

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'REVIEW_APPROVED', 'REVIEW_APPROVED_EMAIL', 'EMAIL',
       'Your review has been approved!',
       'Good news!\n\nYour review for {{productName}} has been approved and is now visible to other customers.'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template WHERE event = 'REVIEW_APPROVED'
);

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'REVIEW_REJECTED', 'REVIEW_REJECTED_EMAIL', 'EMAIL',
       'Your review could not be published',
       'Your review for {{productName}} could not be published as it did not meet our content guidelines.'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template WHERE event = 'REVIEW_REJECTED'
);

-- ===============================
-- RATING EVENTS
-- ===============================

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'RATING_SUBMITTED', 'RATING_SUBMITTED_PUSH', 'PUSH',
       NULL,
       'Thanks for rating {{productName}}! You gave it {{rating}} stars.'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template WHERE event = 'RATING_SUBMITTED'
);

-- ===============================
-- SUPPORT / TICKET EVENTS
-- ===============================

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'TICKET_CREATED', 'TICKET_CREATED_EMAIL', 'EMAIL',
       'Support ticket created',
       'Your support ticket {{ticketId}} has been created.\n\nOur team will get back to you shortly.'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template WHERE event = 'TICKET_CREATED'
);

INSERT INTO notification_template (event, name, channel, subject, content)
SELECT 'TICKET_RESOLVED', 'TICKET_RESOLVED_EMAIL', 'EMAIL',
       'Support ticket resolved',
       'Your support ticket {{ticketId}} has been resolved.\n\nResolution: {{resolutionSummary}}'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template WHERE event = 'TICKET_RESOLVED'
);