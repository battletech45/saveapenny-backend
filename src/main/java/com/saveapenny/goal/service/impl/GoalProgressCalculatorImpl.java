package com.saveapenny.goal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.goal.config.GoalProgressProperties;
import com.saveapenny.goal.dto.GoalDetailResponse;
import com.saveapenny.goal.dto.GoalRunResponse;
import com.saveapenny.goal.dto.ScenarioResponse;
import com.saveapenny.goal.service.GoalProgressCalculator;
import com.saveapenny.goal.service.GoalProgressReport;
import com.saveapenny.goal.service.GoalService;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GoalProgressCalculatorImpl implements GoalProgressCalculator {

    private final GoalService goalService;
    private final GoalProgressProperties properties;

    public GoalProgressCalculatorImpl(GoalService goalService, GoalProgressProperties properties) {
        this.goalService = goalService;
        this.properties = properties;
    }

    @Override
    public GoalProgressReport calculate(UUID userId, UUID goalId, LocalDate asOf) {
        GoalDetailResponse goal = goalService.getById(userId, goalId);
        ScenarioResponse baseline = goal.getScenarios() == null ? null : goal.getScenarios().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsBaseline()))
                .findFirst()
                .orElse(null);
        List<GoalProgressReport.Warning> warnings = new ArrayList<>();
        BigDecimal currentAmount = extractCurrentAmount(goal.getInputs(), warnings);
        int monthsRemaining = monthsRemaining(asOf, goal.getTargetDate());

        if (goal.getTargetAmount() != null && goal.getTargetAmount().signum() > 0 && currentAmount.compareTo(goal.getTargetAmount()) >= 0) {
            return report(goalId, baseline, goal.getLatestRun(), currentAmount, goal.getTargetAmount(), BigDecimal.ZERO,
                    monthsRemaining, GoalProgressReport.ProgressStatus.ACHIEVED, 0, warnings);
        }

        if (goal.getTargetDate() != null && goal.getTargetDate().isBefore(asOf)) {
            warnings.add(new GoalProgressReport.Warning("TARGET_DATE_PASSED", "Goal target date has already passed."));
            return report(goalId, baseline, goal.getLatestRun(), currentAmount, goal.getTargetAmount(),
                    goal.getTargetAmount().subtract(currentAmount), monthsRemaining,
                    GoalProgressReport.ProgressStatus.OFF_TRACK, 1, warnings);
        }

        GoalRunResponse baselineRun = resolveBaselineRun(goal, baseline);
        if (baseline == null || baselineRun == null) {
            warnings.add(new GoalProgressReport.Warning("NO_PROJECTION", "No baseline scenario with a run exists for this goal."));
            return report(goalId, baseline, null, currentAmount, null, null,
                    monthsRemaining, GoalProgressReport.ProgressStatus.NO_PROJECTION, 0, warnings);
        }

        BigDecimal projectedAmountAtTarget = extractProjectedAmount(baselineRun.getOutputSummary());
        BigDecimal gap = projectedAmountAtTarget == null ? null : goal.getTargetAmount().subtract(projectedAmountAtTarget);
        GoalProgressReport.ProgressStatus status = classify(currentAmount, goal.getTargetAmount(), projectedAmountAtTarget);
        int offTrackCount = status == GoalProgressReport.ProgressStatus.OFF_TRACK ? 1 : 0;
        return report(goalId, baseline, baselineRun, currentAmount, projectedAmountAtTarget, gap,
                monthsRemaining, status, offTrackCount, warnings);
    }

    private GoalRunResponse resolveBaselineRun(GoalDetailResponse goal, ScenarioResponse baseline) {
        GoalRunResponse latestRun = goal.getLatestRun();
        if (baseline == null || latestRun == null) {
            return null;
        }
        return baseline.getId().equals(latestRun.getScenarioId()) ? latestRun : null;
    }

    private BigDecimal extractCurrentAmount(JsonNode inputs, List<GoalProgressReport.Warning> warnings) {
        if (inputs != null) {
            JsonNode values = inputs.get("values");
            if (values != null && values.isObject()) {
                for (String field : List.of("startBalance", "currentDownPayment", "currentBalance", "currentRetirementSavings", "currentAverageMonthlyNetIncome")) {
                    JsonNode node = values.get(field);
                    if (node != null && node.isNumber()) {
                        return node.decimalValue();
                    }
                }
            }
        }
        warnings.add(new GoalProgressReport.Warning("CURRENT_BALANCE_MISSING", "Current amount was not found in the goal inputs."));
        return BigDecimal.ZERO;
    }

    private BigDecimal extractProjectedAmount(JsonNode outputSummary) {
        if (outputSummary != null && outputSummary.isObject()) {
            for (String field : List.of("projectedAmount", "projectedNestEgg", "projectedMonthlyNetIncome")) {
                JsonNode node = outputSummary.get(field);
                if (node != null && node.isNumber()) {
                    return node.decimalValue();
                }
            }
        }
        return null;
    }

    private GoalProgressReport.ProgressStatus classify(BigDecimal currentAmount, BigDecimal targetAmount, BigDecimal projectedAmountAtTarget) {
        if (targetAmount != null && targetAmount.signum() > 0 && currentAmount.compareTo(targetAmount) >= 0) {
            return GoalProgressReport.ProgressStatus.ACHIEVED;
        }
        if (targetAmount == null || targetAmount.signum() <= 0 || projectedAmountAtTarget == null) {
            return GoalProgressReport.ProgressStatus.NO_PROJECTION;
        }
        BigDecimal deficit = targetAmount.subtract(projectedAmountAtTarget);
        if (deficit.compareTo(BigDecimal.ZERO) <= 0) {
            return GoalProgressReport.ProgressStatus.ON_TRACK;
        }
        BigDecimal denominator = targetAmount.abs().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE : targetAmount.abs();
        BigDecimal ratio = deficit.divide(denominator, MathContext.DECIMAL64);
        if (ratio.compareTo(properties.offTrackRatio()) >= 0) {
            return GoalProgressReport.ProgressStatus.OFF_TRACK;
        }
        if (ratio.compareTo(properties.atRiskRatio()) >= 0) {
            return GoalProgressReport.ProgressStatus.AT_RISK;
        }
        return GoalProgressReport.ProgressStatus.ON_TRACK;
    }

    private int monthsRemaining(LocalDate asOf, LocalDate targetDate) {
        if (targetDate == null) {
            return 0;
        }
        return Math.max((int) ChronoUnit.MONTHS.between(asOf.withDayOfMonth(1), targetDate.withDayOfMonth(1)), 0);
    }

    private GoalProgressReport report(
            UUID goalId,
            ScenarioResponse baseline,
            GoalRunResponse baselineRun,
            BigDecimal currentAmount,
            BigDecimal projectedAmountAtTarget,
            BigDecimal gap,
            Integer monthsRemaining,
            GoalProgressReport.ProgressStatus status,
            int offTrackForMonthsCount,
            List<GoalProgressReport.Warning> warnings) {
        return new GoalProgressReport(
                goalId,
                baseline == null ? null : baseline.getId(),
                baselineRun == null ? null : baselineRun.getId(),
                currentAmount,
                projectedAmountAtTarget,
                gap,
                monthsRemaining,
                status,
                offTrackForMonthsCount,
                List.copyOf(warnings));
    }
}
