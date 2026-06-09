package com.saveapenny.mcp.goal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GetGoalProgressToolResult(
        UUID goalId,
        UUID baselineRunId,
        BigDecimal currentAmount,
        BigDecimal projectedAmountAtTarget,
        BigDecimal gap,
        Integer monthsRemaining,
        Integer offTrackForMonthsCount,
        GoalToolModels.ProgressStatus status,
        List<GoalToolModels.GoalWarning> warnings) {}
