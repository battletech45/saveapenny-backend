ALTER TABLE feedback ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN';
ALTER TABLE feedback ADD CONSTRAINT chk_feedback_status
    CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'REJECTED'));

CREATE INDEX idx_feedback_status ON feedback (status);
