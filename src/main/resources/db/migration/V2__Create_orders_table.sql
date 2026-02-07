CREATE TABLE orders
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    product_id  VARCHAR(255) NOT NULL,
    side        VARCHAR(32)  NOT NULL,
    status      VARCHAR(64)  NOT NULL,
    occurred_at TIMESTAMP    NOT NULL,
    quantity    DECIMAL(38, 18),
    price       DECIMAL(38, 18),
    fee         DECIMAL(38, 18),
    total       DECIMAL(38, 18),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_orders_user_external UNIQUE (user_id, external_id)
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_user_id_occurred_at ON orders (user_id, occurred_at);


