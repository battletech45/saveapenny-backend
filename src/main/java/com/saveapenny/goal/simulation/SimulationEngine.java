package com.saveapenny.goal.simulation;

import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.simulation.strategy.DebtPayoffGoalStrategy;
import com.saveapenny.goal.simulation.strategy.GoalSimulationStrategy;
import com.saveapenny.goal.simulation.strategy.IncomeTargetGoalStrategy;
import com.saveapenny.goal.simulation.strategy.PurchasePlanningGoalStrategy;
import com.saveapenny.goal.simulation.strategy.RetirementGoalStrategy;
import com.saveapenny.goal.simulation.strategy.SavingsGoalStrategy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SimulationEngine {

    private final Map<GoalType, GoalSimulationStrategy> strategies;

    public SimulationEngine(List<GoalSimulationStrategy> strategies) {
        this.strategies = new EnumMap<>(GoalType.class);
        for (GoalSimulationStrategy strategy : strategies) {
            this.strategies.put(strategy.supports(), strategy);
        }
    }

    public static SimulationEngine defaultEngine() {
        return new SimulationEngine(List.of(
                new SavingsGoalStrategy(),
                new DebtPayoffGoalStrategy(),
                new PurchasePlanningGoalStrategy(),
                new RetirementGoalStrategy(),
                new IncomeTargetGoalStrategy()));
    }

    public SimulationResult simulate(SimulationInput input) {
        GoalSimulationStrategy strategy = strategies.get(input.getType());
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy registered for goal type: " + input.getType());
        }
        return strategy.simulate(input);
    }
}
