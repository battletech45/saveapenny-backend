CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    period VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT chk_budgets_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_budgets_period CHECK (period IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT chk_budgets_date_range CHECK (start_date <= end_date),
    CONSTRAINT uq_budgets_user_category_period_dates UNIQUE (user_id, category_id, period, start_date, end_date)
);

CREATE INDEX idx_budgets_user_id ON budgets (user_id);
CREATE INDEX idx_budgets_user_period ON budgets (user_id, period);
CREATE INDEX idx_budgets_user_category_dates ON budgets (user_id, category_id, start_date, end_date);
