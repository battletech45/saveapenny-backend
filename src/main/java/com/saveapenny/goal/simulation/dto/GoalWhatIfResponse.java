package com.saveapenny.goal.simulation.dto;

import com.saveapenny.goal.simulation.SimulationResult;
import java.math.BigDecimal;
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
public class GoalWhatIfResponse {

    private UUID goalId;
    private SimulationResult result;
    private DeltaVsBaseline deltaVsBaseline;
    private boolean projection;
    private String disclaimer;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeltaVsBaseline {
        private BigDecimal requiredMonthlyContributionDelta;
        private BigDecimal projectedAmountDelta;
        private BigDecimal shortfallDelta;
    }
}
