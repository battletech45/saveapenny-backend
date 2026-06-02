package com.saveapenny.assistant.tool;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AssistantToolContextHolder {

    private final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();

    public void setCurrentUserId(UUID userId) {
        currentUserId.set(userId);
    }

    public UUID requireCurrentUserId() {
        UUID userId = currentUserId.get();
        if (userId == null) {
            throw new IllegalStateException("Assistant tool context is missing authenticated user id.");
        }
        return userId;
    }

    public void clear() {
        currentUserId.remove();
    }
}
