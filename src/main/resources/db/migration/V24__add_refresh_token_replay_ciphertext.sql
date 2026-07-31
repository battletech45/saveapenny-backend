ALTER TABLE refresh_tokens
    ADD COLUMN replacement_token_ciphertext VARCHAR(512),
    ADD COLUMN replacement_token_available_until TIMESTAMPTZ;
