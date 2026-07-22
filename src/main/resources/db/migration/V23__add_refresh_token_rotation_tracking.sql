ALTER TABLE refresh_tokens
    ADD COLUMN revoked_at TIMESTAMPTZ,
    ADD COLUMN replaced_by_token_id UUID REFERENCES refresh_tokens (id),
    ADD COLUMN family_id UUID;

UPDATE refresh_tokens SET family_id = id WHERE family_id IS NULL;

ALTER TABLE refresh_tokens ALTER COLUMN family_id SET NOT NULL;

CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
