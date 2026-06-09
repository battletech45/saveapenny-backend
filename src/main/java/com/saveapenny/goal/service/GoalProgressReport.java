package com.saveapenny.goal.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GoalProgressReport(
        UUID goalId,
        UUID baselineScenarioId,
        UUID baselineRunId,
        BigDecimal currentAmount,
        BigDecimal projectedAmountAtTarget,
        BigDecimal gap,
        Integer monthsRemaining,
        ProgressStatus status,
        int offTrackForMonthsCount,
        List<Warning> warnings) {

    public enum ProgressStatus {
        ON_TRACK,
        AT_RISK,
        OFF_TRACK,
        ACHIEVED,
        NO_PROJECTION
    }

    public record Warning(String code, String message) {}

    public GoalProgressReport withOffTrackForMonthsCount(int count) {
        return new GoalProgressReport(
                goalId,
                baselineScenarioId,
                baselineRunId,
                currentAmount,
                projectedAmountAtTarget,
                gap,
                monthsRemaining,
                status,
                count,
                warnings);
    }
}
