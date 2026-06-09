package com.saveapenny.mcp.goal;

import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;

public record ListGoalsToolInput(GoalStatus status, GoalType type, Integer limit) {

    public int normalizedLimit() {
        return Math.max(1, limit == null ? 10 : limit);
    }
}
