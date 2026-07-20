CREATE TABLE device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token TEXT NOT NULL,
    platform VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_device_tokens_platform CHECK (platform IN ('ANDROID', 'IOS'))
);

CREATE UNIQUE INDEX uq_device_tokens_token ON device_tokens (token);
CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);
