CREATE TABLE insights (
    id            UUID           PRIMARY KEY,
    user_id       UUID           NOT NULL,
    type          VARCHAR(32)    NOT NULL,
    title         VARCHAR(200)   NOT NULL,
    summary       TEXT           NOT NULL,
    detail        TEXT,
    category_id   UUID,
    severity      VARCHAR(20)    NOT NULL DEFAULT 'INFO',
    metadata      TEXT,
    is_read       BOOLEAN        NOT NULL DEFAULT FALSE,
    is_dismissed  BOOLEAN        NOT NULL DEFAULT FALSE,
    generated_at  TIMESTAMP      NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_insights_user_id      ON insights (user_id);
CREATE INDEX idx_insights_type         ON insights (type);
CREATE INDEX idx_insights_severity     ON insights (severity);
CREATE INDEX idx_insights_generated_at ON insights (generated_at);
