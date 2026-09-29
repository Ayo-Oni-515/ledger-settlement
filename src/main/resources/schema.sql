CREATE TABLE IF NOT EXISTS payments (
    id           VARCHAR(64) PRIMARY KEY,
    merchant_id  VARCHAR(64) NOT NULL,
    amount_minor BIGINT      NOT NULL,
    currency     VARCHAR(3)  NOT NULL,
    recorded_at  TIMESTAMPTZ NOT NULL
);