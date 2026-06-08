package com.saveapenny.goal.service;

import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.goal.simulation.dto.CompareScenariosRequest;
import com.saveapenny.goal.simulation.dto.DraftGoalSimulationRequest;
import com.saveapenny.goal.simulation.dto.GoalScenarioComparisonResponse;
import com.saveapenny.goal.simulation.dto.GoalSimulationResponse;
import com.saveapenny.goal.simulation.dto.GoalWhatIfResponse;
import com.saveapenny.goal.simulation.dto.WhatIfRequest;
import java.util.UUID;

public interface GoalSimulationService {

    SimulationResult simulateGoal(UUID currentUserId, UUID goalId, UUID scenarioId);

    GoalSimulationResponse simulateDraft(UUID currentUserId, DraftGoalSimulationRequest request);

    GoalSimulationResponse simulatePrompt(UUID currentUserId, String prompt);

    GoalScenarioComparisonResponse compareScenarios(UUID currentUserId, UUID goalId, CompareScenariosRequest request);

    GoalWhatIfResponse whatIf(UUID currentUserId, UUID goalId, WhatIfRequest request);
}
