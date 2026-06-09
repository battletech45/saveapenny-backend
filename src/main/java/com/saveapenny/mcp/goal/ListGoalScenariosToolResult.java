package com.saveapenny.mcp.goal;

import java.util.List;
import java.util.UUID;

public record ListGoalScenariosToolResult(UUID goalId, List<GoalToolModels.ScenarioItem> scenarios) {}
