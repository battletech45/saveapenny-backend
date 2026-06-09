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

class DebtPayoffGoalStrategyTest {

    private final DebtPayoffGoalStrategy strategy = new DebtPayoffGoalStrategy();

    @Test
    void simulate_returnsOnTrack_whenDebtCanBePaidComfortably() {
        SimulationResult result = strategy.simulate(baseInput()
                .currentBalance(new BigDecimal("8000"))
                .apr(new BigDecimal("12"))
                .fixedPayment(new BigDecimal("500"))
                .averageMonthlyNetIncome(new BigDecimal("4000"))
                .build());

        assertEquals(Feasibility.ON_TRACK, result.getFeasibility());
    }

    @Test
    void simulate_returnsTight_whenRequiredPaymentUsesMiddleIncomeBand() {
        SimulationResult result = strategy.simulate(baseInput()
                .currentBalance(new BigDecimal("12000"))
                .apr(new BigDecimal("18"))
                .targetPayoffDate(LocalDate.of(2029, 6, 1))
                .fixedPayment(new BigDecimal("450"))
                .averageMonthlyNetIncome(new BigDecimal("1200"))
                .build());

        assertEquals(Feasibility.TIGHT, result.getFeasibility());
    }

    @Test
    void simulate_returnsInfeasible_whenPaymentDoesNotCoverInterest() {
        SimulationResult result = strategy.simulate(baseInput()
                .currentBalance(new BigDecimal("10000"))
                .apr(new BigDecimal("36"))
                .fixedPayment(new BigDecimal("100"))
                .averageMonthlyNetIncome(new BigDecimal("3000"))
                .build());

        assertEquals(Feasibility.INFEASIBLE, result.getFeasibility());
        assertTrue(result.getWarnings().stream().anyMatch(item -> item.getCode().equals("HIGH_APR")));
    }

    private SimulationInput.SimulationInputBuilder baseInput() {
        return SimulationInput.builder()
                .type(GoalType.DEBT_PAYOFF)
                .asOfDate(LocalDate.of(2026, 6, 1))
                .targetPayoffDate(LocalDate.of(2028, 6, 1))
                .currency("USD");
    }
}
