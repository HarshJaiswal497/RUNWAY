-- 5. Create Wishlist Table
CREATE TABLE wishlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uq_wishlist_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_wishlist_user_id ON wishlist(user_id);