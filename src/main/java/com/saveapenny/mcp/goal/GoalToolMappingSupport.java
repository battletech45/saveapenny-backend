package com.saveapenny.mcp.goal;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.goal.dto.GoalDetailResponse;
import com.saveapenny.goal.dto.GoalResponse;
import com.saveapenny.goal.dto.GoalRunResponse;
import com.saveapenny.goal.dto.ScenarioResponse;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.mcp.error.ToolError;
import com.saveapenny.mcp.error.ToolErrorCode;
import com.saveapenny.mcp.error.ToolExecutionException;
import com.saveapenny.mcp.error.ToolValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

final class GoalToolMappingSupport {

    private GoalToolMappingSupport() {}

    static GoalToolModels.GoalItem toGoalItem(GoalResponse goal) {
        return new GoalToolModels.GoalItem(
                goal.getId(),
                goal.getType(),
                goal.getTitle(),
                goal.getStatus(),
                goal.getTargetAmount(),
                goal.getCurrency(),
                goal.getTargetDate());
    }

    static GoalToolModels.GoalDetailItem toGoalDetailItem(GoalDetailResponse goal) {
        return new GoalToolModels.GoalDetailItem(
                goal.getId(),
                goal.getType(),
                goal.getTitle(),
                goal.getStatus(),
                goal.getTargetAmount(),
                goal.getCurrency(),
                goal.getTargetDate(),
                goal.getScenarios() == null ? List.of() : goal.getScenarios().stream().map(GoalToolMappingSupport::toScenarioItem).toList(),
                goal.getLatestRun() == null ? null : toGoalRunItem(goal.getLatestRun()));
    }

    static GoalToolModels.ScenarioItem toScenarioItem(ScenarioResponse scenario) {
        return new GoalToolModels.ScenarioItem(
                scenario.getId(),
                scenario.getName(),
                Boolean.TRUE.equals(scenario.getIsBaseline()),
                scenario.getCreatedAt());
    }

    static GoalToolModels.GoalRunItem toGoalRunItem(GoalRunResponse run) {
        return new GoalToolModels.GoalRunItem(
                run.getId(),
                run.getScenarioId(),
                run.getFeasibility(),
                run.getTriggeredBy(),
                run.getCreatedAt());
    }

    static void requireGoalId(UUID goalId, String fieldName) {
        if (goalId == null) {
            throw new ToolValidationException(
                    fieldName + " is required.",
                    List.of(new ToolError(null, fieldName + " is required.", fieldName)));
        }
    }

    static ToolExecutionException notFound(String message) {
        return new ToolExecutionException(ToolErrorCode.NOT_FOUND, message);
    }

    static BigDecimal extractCurrentAmount(JsonNode inputs) {
        JsonNode values = inputs == null ? null : inputs.get("values");
        if (values == null || !values.isObject()) {
            return BigDecimal.ZERO;
        }
        for (String field : List.of(
                "startBalance",
                "currentDownPayment",
                "currentBalance",
                "currentRetirementSavings",
                "currentAverageMonthlyNetIncome")) {
            JsonNode node = values.get(field);
            if (node != null && node.isNumber()) {
                return node.decimalValue();
            }
        }
        return BigDecimal.ZERO;
    }

    static BigDecimal extractProjectedAmountAtTarget(GoalRunResponse latestRun) {
        if (latestRun == null || latestRun.getOutputSummary() == null || !latestRun.getOutputSummary().isObject()) {
            return null;
        }
        JsonNode summary = latestRun.getOutputSummary();
        for (String field : List.of("projectedAmount", "projectedNestEgg", "projectedMonthlyNetIncome")) {
            JsonNode node = summary.get(field);
            if (node != null && node.isNumber()) {
                return node.decimalValue();
            }
        }
        return null;
    }

    static GoalToolModels.ProgressStatus classifyProgress(
            GoalStatus goalStatus,
            BigDecimal currentAmount,
            BigDecimal targetAmount,
            BigDecimal projectedAmountAtTarget) {
        if (goalStatus == GoalStatus.ACHIEVED || currentAmount.compareTo(targetAmount) >= 0) {
            return GoalToolModels.ProgressStatus.ACHIEVED;
        }
        if (projectedAmountAtTarget == null) {
            return GoalToolModels.ProgressStatus.NO_PROJECTION;
        }
        BigDecimal gap = targetAmount.subtract(projectedAmountAtTarget);
        if (gap.compareTo(BigDecimal.ZERO) <= 0) {
            return GoalToolModels.ProgressStatus.ON_TRACK;
        }
        BigDecimal ratio = gap.divide(targetAmount.abs().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : targetAmount.abs(),
                java.math.MathContext.DECIMAL64);
        if (ratio.compareTo(new BigDecimal("0.10")) >= 0) {
            return GoalToolModels.ProgressStatus.OFF_TRACK;
        }
        if (ratio.compareTo(new BigDecimal("0.05")) >= 0) {
            return GoalToolModels.ProgressStatus.AT_RISK;
        }
        return GoalToolModels.ProgressStatus.ON_TRACK;
    }

    static int monthsRemaining(LocalDate targetDate) {
        if (targetDate == null) {
            return 0;
        }
        return Math.max((int) java.time.temporal.ChronoUnit.MONTHS.between(LocalDate.now().withDayOfMonth(1), targetDate.withDayOfMonth(1)), 0);
    }
}
