CREATE TABLE credit_card_details (
    account_id UUID PRIMARY KEY,
    credit_limit NUMERIC(19,4) NOT NULL,
    apr NUMERIC(6,3) NOT NULL,
    statement_day INT NOT NULL,
    grace_period_days INT NOT NULL DEFAULT 21,
    last_statement_date DATE,
    next_statement_date DATE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_credit_card_details_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT chk_credit_card_details_limit_positive CHECK (credit_limit > 0),
    CONSTRAINT chk_credit_card_details_apr_non_negative CHECK (apr >= 0),
    CONSTRAINT chk_credit_card_details_statement_day CHECK (statement_day BETWEEN 1 AND 28),
    CONSTRAINT chk_credit_card_details_grace_period_positive CHECK (grace_period_days > 0)
);

CREATE TABLE credit_card_statements (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    user_id UUID NOT NULL,
    statement_date DATE NOT NULL,
    due_date DATE NOT NULL,
    previous_balance NUMERIC(19,4) NOT NULL,
    new_balance NUMERIC(19,4) NOT NULL,
    interest_charged NUMERIC(19,4) NOT NULL DEFAULT 0,
    minimum_payment_due NUMERIC(19,4) NOT NULL DEFAULT 0,
    amount_paid NUMERIC(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_credit_card_statements_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_credit_card_statements_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_credit_card_statements_account_date UNIQUE (account_id, statement_date),
    CONSTRAINT chk_credit_card_statements_status CHECK (status IN ('OPEN', 'PAID', 'MISSED'))
);

CREATE INDEX idx_credit_card_statements_account_id ON credit_card_statements (account_id);
CREATE INDEX idx_credit_card_statements_account_due_date ON credit_card_statements (account_id, due_date);
CREATE INDEX idx_credit_card_statements_user_id ON credit_card_statements (user_id);

INSERT INTO categories (id, user_id, name, type, color, icon, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000f1', NULL, 'Interest & Fees', 'EXPENSE', '#EF4444', 'percent', NOW(), NOW()),
    ('00000000-0000-0000-0000-0000000000f2', NULL, 'Credit Card Payment', 'EXPENSE', '#10B981', 'credit-card', NOW(), NOW());
