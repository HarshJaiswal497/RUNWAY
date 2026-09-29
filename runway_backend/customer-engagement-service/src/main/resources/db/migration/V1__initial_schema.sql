-- ======================
-- NOTIFICATION TEMPLATE
-- ======================
CREATE TABLE notification_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    subject VARCHAR(255),
    channel VARCHAR(50),
    content LONGTEXT,
    created_at TIMESTAMP,
    event VARCHAR(100) NOT NULL,
    CONSTRAINT uk_notification_template_event UNIQUE (event)
);

-- ======================
-- NOTIFICATION LOG
-- ======================
CREATE TABLE notification_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event VARCHAR(50) NOT NULL,
    template_id BIGINT NOT NULL,
    channel VARCHAR(20),
    status VARCHAR(20),
    payload TEXT,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    CONSTRAINT fk_notification_template
        FOREIGN KEY (template_id)
        REFERENCES notification_template(id)
);

-- ======================
-- RATING
-- ======================
CREATE TABLE rating (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT uk_rating_product_user UNIQUE (product_id, user_id)
);

-- ======================
-- REVIEW
-- ======================
CREATE TABLE review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT,
    title VARCHAR(255),
    body LONGTEXT,
    status VARCHAR(50),
    created_at TIMESTAMP
);

-- ======================
-- TICKET
-- ======================
CREATE TABLE ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    subject VARCHAR(255),
    priority VARCHAR(50),
    status VARCHAR(50),
    assigned_to BIGINT,
    order_id BIGINT,
    created_at TIMESTAMP
);

-- ======================
-- TICKET MESSAGE
-- ======================
CREATE TABLE ticket_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message LONGTEXT,
    sender_role VARCHAR(50),
    created_at TIMESTAMP
);