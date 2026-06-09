package com.saveapenny.mcp.goal;

import com.saveapenny.goal.exception.GoalNotFoundException;
import com.saveapenny.goal.exception.ScenarioNotFoundException;
import com.saveapenny.goal.service.GoalSimulationService;
import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.mcp.definition.ToolDataType;
import com.saveapenny.mcp.definition.ToolDefinition;
import com.saveapenny.mcp.definition.ToolPropertyDefinition;
import com.saveapenny.mcp.definition.ToolSchemaDefinition;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import com.saveapenny.mcp.execution.ToolHandler;
import com.saveapenny.mcp.execution.ToolResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SimulateGoalToolHandler implements ToolHandler<SimulateGoalToolInput, SimulationResult> {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "simulate_goal",
            "Run a live simulation for an existing goal and optional scenario.",
            new ToolSchemaDefinition(
                    "SimulateGoalToolInput",
                    ToolDataType.OBJECT,
                    "Input for goal simulation.",
                    List.of(
                            new ToolPropertyDefinition("goalId", ToolDataType.STRING, "Goal id.", true),
                            new ToolPropertyDefinition("scenarioId", ToolDataType.STRING, "Optional scenario id.", false))),
            new ToolSchemaDefinition(
                    "SimulationResult",
                    ToolDataType.OBJECT,
                    "Live simulation result.",
                    List.of(new ToolPropertyDefinition("feasibility", ToolDataType.STRING, "Simulation feasibility.", true))),
            SimulateGoalToolInput.class,
            SimulationResult.class);

    private final GoalSimulationService goalSimulationService;

    public SimulateGoalToolHandler(GoalSimulationService goalSimulationService) {
        this.goalSimulationService = goalSimulationService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(ToolExecutionContext context, SimulateGoalToolInput input) {
        GoalToolMappingSupport.requireGoalId(input == null ? null : input.goalId(), "goalId");
    }

    @Override
    public ToolResult<SimulationResult> doExecute(ToolExecutionContext context, SimulateGoalToolInput input) {
        try {
            return ToolResult.of(goalSimulationService.simulateGoal(context.requireUserId(), input.goalId(), input.scenarioId()));
        } catch (GoalNotFoundException | ScenarioNotFoundException ex) {
            throw GoalToolMappingSupport.notFound(ex.getMessage());
        }
    }
}
