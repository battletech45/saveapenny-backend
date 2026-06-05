package com.saveapenny.mcp.execution;

import java.util.UUID;

public record ToolExecutionContext(UUID userId) {

    public UUID requireUserId() {
        if (userId == null) {
            throw new IllegalStateException("Tool execution context is missing authenticated user id.");
        }
        return userId;
    }
}
