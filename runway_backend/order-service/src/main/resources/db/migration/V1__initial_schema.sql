-- =============================================================
--  V1__initial_schema.sql
--  Order Service - Initial Database Schema
--  Tables: carts, cart_items, orders, order_lines, shipments
-- =============================================================

CREATE TABLE IF NOT EXISTS carts
(
    id         BIGINT    NOT NULL AUTO_INCREMENT,
    user_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_carts_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cart_items
(
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    cart_id       BIGINT         NOT NULL,
    product_id    BIGINT         NOT NULL,
    product_title VARCHAR(255)   NOT NULL,
    quantity      INT            NOT NULL DEFAULT 1,
    unit_price    DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    UNIQUE KEY uq_cart_product (cart_id, product_id),
    INDEX idx_cart_items_cart (cart_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS orders
(
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    order_number     VARCHAR(50)    NOT NULL,
    user_id          BIGINT         NOT NULL,
    status           VARCHAR(30)    NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount     DECIMAL(12, 2) NOT NULL,
    currency         VARCHAR(10)    NOT NULL DEFAULT 'INR',
    shipping_address VARCHAR(500)   NOT NULL,
    notes            VARCHAR(500),
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_order_number (order_number),
    INDEX idx_orders_user_id (user_id),
    INDEX idx_orders_status  (status),
    INDEX idx_orders_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_lines
(
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    order_id      BIGINT         NOT NULL,
    product_id    BIGINT         NOT NULL,
    product_title VARCHAR(255)   NOT NULL,
    quantity      INT            NOT NULL,
    unit_price    DECIMAL(12, 2) NOT NULL,
    line_total    DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    INDEX idx_order_lines_order   (order_id),
    INDEX idx_order_lines_product (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shipments
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    order_id         BIGINT       NOT NULL,
    carrier          VARCHAR(100),
    tracking_number  VARCHAR(100),
    status           VARCHAR(30)  NOT NULL DEFAULT 'PROCESSING',
    shipping_address VARCHAR(500) NOT NULL,
    shipped_at       TIMESTAMP    NULL,
    delivered_at     TIMESTAMP    NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_shipments_order (order_id),
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    INDEX idx_shipments_tracking (tracking_number),
    INDEX idx_shipments_status   (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
