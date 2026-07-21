CREATE TABLE billing_customer (
    user_id UUID PRIMARY KEY,
    revenuecat_app_user_id VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_billing_customer_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE billing_entitlement (
    user_id UUID PRIMARY KEY,
    plan VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    store VARCHAR(20),
    product_id VARCHAR(100),
    entitlement_id VARCHAR(100),
    expires_at TIMESTAMPTZ,
    trial_ends_at TIMESTAMPTZ,
    grace_period_ends_at TIMESTAMPTZ,
    will_renew BOOLEAN NOT NULL DEFAULT FALSE,
    last_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_billing_entitlement_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_billing_entitlement_plan CHECK (plan IN ('FREE', 'PLUS')),
    CONSTRAINT chk_billing_entitlement_status CHECK (
        status IN ('INACTIVE', 'TRIALING', 'ACTIVE', 'GRACE_PERIOD', 'CANCELED', 'EXPIRED')
    )
);

CREATE INDEX idx_billing_entitlement_status ON billing_entitlement (status);
