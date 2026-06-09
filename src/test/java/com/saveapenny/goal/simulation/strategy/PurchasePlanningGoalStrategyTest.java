package com.saveapenny.goal.simulation.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.simulation.SimulationInput;
import com.saveapenny.goal.simulation.SimulationResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PurchasePlanningGoalStrategyTest {

    private final PurchasePlanningGoalStrategy strategy = new PurchasePlanningGoalStrategy();

    @Test
    void simulate_returnsOnTrack_forReachableDownPayment() {
        SimulationResult result = strategy.simulate(baseInput()
                .targetPrice(new BigDecimal("300000"))
                .currentDownPayment(new BigDecimal("40000"))
                .monthlySaving(new BigDecimal("2000"))
                .averageMonthlyNetIncome(new BigDecimal("7000"))
                .build());

        assertEquals(Feasibility.ON_TRACK, result.getFeasibility());
    }

    @Test
    void simulate_returnsTight_forModerateIncomePressure() {
        SimulationResult result = strategy.simulate(baseInput()
                .targetPrice(new BigDecimal("250000"))
                .currentDownPayment(BigDecimal.ZERO)
                .monthlySaving(new BigDecimal("200"))
                .averageMonthlyNetIncome(new BigDecimal("2000"))
                .build());

        assertEquals(Feasibility.TIGHT, result.getFeasibility());
    }

    @Test
    void simulate_returnsInfeasible_forVeryLargePurchaseGoal() {
        SimulationResult result = strategy.simulate(baseInput()
                .targetPrice(new BigDecimal("500000"))
                .currentDownPayment(BigDecimal.ZERO)
                .monthlySaving(BigDecimal.ZERO)
                .averageMonthlyNetIncome(new BigDecimal("1500"))
                .build());

        assertEquals(Feasibility.INFEASIBLE, result.getFeasibility());
    }

    private SimulationInput.SimulationInputBuilder baseInput() {
        return SimulationInput.builder()
                .type(GoalType.PURCHASE)
                .asOfDate(LocalDate.of(2026, 6, 1))
                .targetDate(LocalDate.of(2031, 6, 1))
                .currency("USD")
                .downPaymentPercent(new BigDecimal("20"))
                .expectedAnnualReturn(BigDecimal.ZERO)
                .expectedPriceInflation(BigDecimal.ZERO);
    }
}
