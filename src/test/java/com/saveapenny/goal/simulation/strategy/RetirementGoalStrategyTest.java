package com.saveapenny.goal.simulation.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.simulation.SimulationInput;
import com.saveapenny.goal.simulation.SimulationResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RetirementGoalStrategyTest {

    private final RetirementGoalStrategy strategy = new RetirementGoalStrategy();

    @Test
    void simulate_returnsOnTrack_forStrongRetirementProjection() {
        SimulationResult result = strategy.simulate(baseInput()
                .currentRetirementSavings(new BigDecimal("250000"))
                .monthlyContribution(new BigDecimal("1500"))
                .desiredMonthlyIncomeInRetirement(new BigDecimal("3000"))
                .build());

        assertEquals(Feasibility.ON_TRACK, result.getFeasibility());
    }

    @Test
    void simulate_returnsTight_whenProjectionNearlyMeetsNestEgg() {
        SimulationResult result = strategy.simulate(baseInput()
                .currentRetirementSavings(new BigDecimal("100000"))
                .monthlyContribution(new BigDecimal("850"))
                .desiredMonthlyIncomeInRetirement(new BigDecimal("2600"))
                .build());

        assertEquals(Feasibility.TIGHT, result.getFeasibility());
    }

    @Test
    void simulate_returnsInfeasible_whenProjectedNestEggIsFarShort() {
        SimulationResult result = strategy.simulate(baseInput()
                .currentRetirementSavings(new BigDecimal("10000"))
                .monthlyContribution(new BigDecimal("100"))
                .desiredMonthlyIncomeInRetirement(new BigDecimal("6000"))
                .build());

        assertEquals(Feasibility.INFEASIBLE, result.getFeasibility());
    }

    @Test
    void simulate_reportsImmediateShortfallContribution_whenAlreadyAtRetirementAge() {
        SimulationResult result = strategy.simulate(baseInput()
                .currentAge(65)
                .targetRetirementAge(65)
                .currentRetirementSavings(new BigDecimal("100000"))
                .monthlyContribution(BigDecimal.ZERO)
                .desiredMonthlyIncomeInRetirement(new BigDecimal("3000"))
                .build());

        assertEquals(new BigDecimal("800000.00"), result.getSummary().get("requiredMonthlyContribution"));
        assertEquals(0, result.getHorizonMonths());
    }

    private SimulationInput.SimulationInputBuilder baseInput() {
        return SimulationInput.builder()
                .type(GoalType.RETIREMENT)
                .asOfDate(LocalDate.of(2026, 6, 1))
                .currency("USD")
                .currentAge(35)
                .targetRetirementAge(65)
                .expectedAnnualReturn(new BigDecimal("7"))
                .expectedInflation(new BigDecimal("3"))
                .withdrawalRate(new BigDecimal("4"));
    }
}
