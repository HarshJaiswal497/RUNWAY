CREATE TABLE payment_intent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    razorpay_order_id VARCHAR(255)
);
