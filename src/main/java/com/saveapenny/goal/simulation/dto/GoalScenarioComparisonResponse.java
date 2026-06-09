package com.saveapenny.goal.simulation.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalScenarioComparisonResponse {

    private UUID goalId;
    private List<ScenarioComparisonItem> scenarios;
    private List<ScenarioDeltaItem> deltas;
    private String disclaimer;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioComparisonItem {
        private UUID scenarioId;
        private String scenarioName;
        private boolean isBaseline;
        private String feasibility;
        private int horizonMonths;
        private String currency;
        private BigDecimal requiredMonthlyContribution;
        private BigDecimal projectedAmount;
        private BigDecimal shortfall;
        private int warningsCount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioDeltaItem {
        private UUID fromScenarioId;
        private UUID toScenarioId;
        private boolean feasibilityChanged;
        private BigDecimal requiredMonthlyContributionDelta;
        private BigDecimal projectedAmountDelta;
        private BigDecimal shortfallDelta;
    }
}
