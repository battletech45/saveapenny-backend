package com.saveapenny.goal.simulation.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.simulation.SimulationInput;
import com.saveapenny.goal.simulation.SimulationResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SavingsGoalStrategyTest {

    private final SavingsGoalStrategy strategy = new SavingsGoalStrategy();

    @Test
    void simulate_returnsOnTrack_forComfortableSavingsPlan() {
        SimulationResult result = strategy.simulate(baseInput()
                .targetAmount(new BigDecimal("12000"))
                .monthlyContribution(new BigDecimal("400"))
                .averageMonthlyNetIncome(new BigDecimal("4000"))
                .build());

        assertEquals(GoalType.SAVINGS, result.getType());
        assertEquals(Feasibility.ON_TRACK, result.getFeasibility());
        assertEquals(36, result.getHorizonMonths());
    }

    @Test
    void simulate_returnsTight_whenRequiredContributionIsNearIncomeThreshold() {
        SimulationResult result = strategy.simulate(baseInput()
                .targetAmount(new BigDecimal("12000"))
                .monthlyContribution(new BigDecimal("100"))
                .averageMonthlyNetIncome(new BigDecimal("1000"))
                .build());

        assertEquals(Feasibility.TIGHT, result.getFeasibility());
    }

    @Test
    void simulate_returnsInfeasible_whenRequiredContributionExceedsIncomeBand() {
        SimulationResult result = strategy.simulate(baseInput()
                .targetAmount(new BigDecimal("50000"))
                .monthlyContribution(BigDecimal.ZERO)
                .averageMonthlyNetIncome(new BigDecimal("1000"))
                .build());

        assertEquals(Feasibility.INFEASIBLE, result.getFeasibility());
        assertTrue(result.getSeries().size() > 30);
    }

    private SimulationInput.SimulationInputBuilder baseInput() {
        return SimulationInput.builder()
                .type(GoalType.SAVINGS)
                .asOfDate(LocalDate.of(2026, 6, 1))
                .targetDate(LocalDate.of(2029, 6, 1))
                .currency("USD")
                .startBalance(BigDecimal.ZERO)
                .expectedAnnualReturn(BigDecimal.ZERO);
    }
}
