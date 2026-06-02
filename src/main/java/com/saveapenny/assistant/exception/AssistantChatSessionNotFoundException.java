package com.saveapenny.assistant.exception;

import java.util.UUID;

public class AssistantChatSessionNotFoundException extends RuntimeException {

    public AssistantChatSessionNotFoundException(UUID sessionId) {
        super("Assistant chat session not found: " + sessionId);
    }
}
