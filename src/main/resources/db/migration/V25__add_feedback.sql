CREATE TABLE feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    rating INTEGER,
    message TEXT NOT NULL,
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_feedback_type CHECK (type IN ('GENERAL', 'FEATURE_REQUEST', 'BUG_REPORT')),
    CONSTRAINT chk_feedback_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_feedback_user_id ON feedback (user_id);
CREATE INDEX idx_feedback_user_created ON feedback (user_id, created_at);
