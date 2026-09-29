-- =============================================================
--  V2__seed_sample_data.sql
--  Order Service — Sample Data for Development
--
--  This file only runs in dev profile.
--  Inserts sample carts, orders, and shipments for testing.
--
--  NOTE: user_id values (1, 2, 3) must match users
--  created by Identity Service.
-- =============================================================

-- Sample cart for customer user (userId = 3)
INSERT IGNORE INTO carts (user_id) VALUES (3);

-- Sample cart items
INSERT IGNORE INTO cart_items
    (cart_id, product_id, product_title, quantity, unit_price)
VALUES
    (1, 1, 'Samsung Galaxy S24',    2, 79999.00),
    (1, 3, 'Sony WH-1000XM5',      1, 24999.00);

-- Sample completed order
INSERT IGNORE INTO orders
    (order_number, user_id, status, total_amount, currency, shipping_address)
VALUES
    ('ORD-SAMPLE01', 3, 'DELIVERED', 184997.00, 'INR',
     '123 MG Road, Bangalore, Karnataka - 560001'),
    ('ORD-SAMPLE02', 3, 'PENDING_PAYMENT', 24999.00, 'INR',
     '456 Anna Salai, Chennai, Tamil Nadu - 600002');

-- Sample order lines for ORD-SAMPLE01
INSERT IGNORE INTO order_lines
    (order_id, product_id, product_title, quantity, unit_price, line_total)
SELECT o.id, 1, 'Samsung Galaxy S24', 2, 79999.00, 159998.00
FROM orders o WHERE o.order_number = 'ORD-SAMPLE01';

INSERT IGNORE INTO order_lines
    (order_id, product_id, product_title, quantity, unit_price, line_total)
SELECT o.id, 3, 'Sony WH-1000XM5', 1, 24999.00, 24999.00
FROM orders o WHERE o.order_number = 'ORD-SAMPLE01';

-- Sample shipment for ORD-SAMPLE01
INSERT IGNORE INTO shipments
    (order_id, carrier, tracking_number, status, shipping_address, shipped_at, delivered_at)
SELECT o.id,
       'BlueDart',
       'BD123456789IN',
       'DELIVERED',
       '123 MG Road, Bangalore, Karnataka - 560001',
       DATE_SUB(NOW(), INTERVAL 5 DAY),
       DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM orders o WHERE o.order_number = 'ORD-SAMPLE01';
