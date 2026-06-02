CREATE TABLE assistant_chat_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_assistant_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE assistant_chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_assistant_chat_messages_session FOREIGN KEY (session_id) REFERENCES assistant_chat_sessions (id) ON DELETE CASCADE,
    CONSTRAINT chk_assistant_chat_messages_role CHECK (role IN ('user', 'assistant'))
);

CREATE INDEX idx_assistant_chat_sessions_user_id ON assistant_chat_sessions (user_id);
CREATE INDEX idx_assistant_chat_sessions_updated_at ON assistant_chat_sessions (updated_at);
CREATE INDEX idx_assistant_chat_messages_session_id ON assistant_chat_messages (session_id);
CREATE INDEX idx_assistant_chat_messages_session_created_at ON assistant_chat_messages (session_id, created_at);
