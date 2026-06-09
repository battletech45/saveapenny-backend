package com.saveapenny.goal.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.saveapenny.goal.entity.GoalType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SimulationEngineTest {

    @Test
    void engine_dispatchesToMatchingStrategy() {
        SimulationEngine engine = SimulationEngine.defaultEngine();
        SimulationResult result = engine.simulate(SimulationInput.builder()
                .type(GoalType.SAVINGS)
                .asOfDate(LocalDate.of(2026, 6, 1))
                .targetDate(LocalDate.of(2029, 6, 1))
                .currency("USD")
                .targetAmount(new BigDecimal("12000"))
                .monthlyContribution(new BigDecimal("300"))
                .averageMonthlyNetIncome(new BigDecimal("3000"))
                .build());

        assertEquals(GoalType.SAVINGS, result.getType());
    }
}
