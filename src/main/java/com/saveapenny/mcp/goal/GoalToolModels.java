package com.saveapenny.mcp.goal;

import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalRunTrigger;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class GoalToolModels {

    private GoalToolModels() {}

    public record GoalItem(
            UUID goalId,
            GoalType type,
            String title,
            GoalStatus status,
            BigDecimal targetAmount,
            String currency,
            LocalDate targetDate) {
    }

    public record ScenarioItem(
            UUID scenarioId,
            String name,
            boolean isBaseline,
            OffsetDateTime createdAt) {
    }

    public record GoalRunItem(
            UUID runId,
            UUID scenarioId,
            Feasibility feasibility,
            GoalRunTrigger triggeredBy,
            OffsetDateTime createdAt) {
    }

    public record GoalWarning(String code, String message) {
    }

    public enum ProgressStatus {
        ON_TRACK,
        AT_RISK,
        OFF_TRACK,
        ACHIEVED,
        NO_PROJECTION
    }

    public record GoalDetailItem(
            UUID goalId,
            GoalType type,
            String title,
            GoalStatus status,
            BigDecimal targetAmount,
            String currency,
            LocalDate targetDate,
            List<ScenarioItem> scenarios,
            GoalRunItem latestRun) {
    }
}
