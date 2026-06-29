CREATE TABLE stock_holdings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL,
    purchase_price NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    purchase_date DATE NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_stock_holdings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_stock_holdings_user_id ON stock_holdings (user_id);
CREATE INDEX idx_stock_holdings_user_symbol ON stock_holdings (user_id, symbol);
CREATE UNIQUE INDEX uq_stock_holdings_user_symbol_date
    ON stock_holdings (user_id, symbol, purchase_date);
