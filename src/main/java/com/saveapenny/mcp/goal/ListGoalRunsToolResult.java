package com.saveapenny.mcp.goal;

import java.util.List;
import java.util.UUID;

public record ListGoalRunsToolResult(UUID goalId, List<GoalToolModels.GoalRunItem> runs) {}
