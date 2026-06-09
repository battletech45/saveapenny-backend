package com.saveapenny.goal.simulation.strategy;

import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.simulation.SimulationInput;
import com.saveapenny.goal.simulation.SimulationResult;

public interface GoalSimulationStrategy {

    GoalType supports();

    SimulationResult simulate(SimulationInput input);
}
