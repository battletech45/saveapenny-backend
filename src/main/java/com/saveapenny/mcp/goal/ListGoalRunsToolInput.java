package com.saveapenny.mcp.goal;

import java.util.UUID;

public record ListGoalRunsToolInput(UUID goalId, Integer limit) {

    public int normalizedLimit() {
        return Math.max(1, limit == null ? 10 : limit);
    }
}
