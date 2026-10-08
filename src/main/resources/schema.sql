CREATE TABLE IF NOT EXISTS carts (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    open_user_id BINARY(16) NULL,
    status VARCHAR(24) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_carts_open_user UNIQUE (open_user_id),
    INDEX idx_carts_user_id (user_id),
    CONSTRAINT chk_carts_status CHECK (status IN ('ACTIVE', 'CHECKOUT_PENDING', 'COMPLETED'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cart_items (
    id BINARY(16) NOT NULL,
    cart_id BINARY(16) NOT NULL,
    book_id BINARY(16) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_cart_book UNIQUE (cart_id, book_id),
    CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_cart_items_price CHECK (unit_price >= 0),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE
) ENGINE=InnoDB;
