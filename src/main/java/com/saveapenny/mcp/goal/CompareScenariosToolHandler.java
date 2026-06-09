package com.saveapenny.mcp.goal;

import com.saveapenny.goal.exception.GoalNotFoundException;
import com.saveapenny.goal.exception.ScenarioNotFoundException;
import com.saveapenny.goal.service.GoalSimulationService;
import com.saveapenny.goal.simulation.dto.CompareScenariosRequest;
import com.saveapenny.goal.simulation.dto.GoalScenarioComparisonResponse;
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
public class CompareScenariosToolHandler implements ToolHandler<CompareScenariosToolInput, GoalScenarioComparisonResponse> {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "compare_scenarios",
            "Compare baseline and alternative scenarios for a goal without persistence.",
            new ToolSchemaDefinition(
                    "CompareScenariosToolInput",
                    ToolDataType.OBJECT,
                    "Input for scenario comparison.",
                    List.of(
                            new ToolPropertyDefinition("goalId", ToolDataType.STRING, "Goal id.", true),
                            new ToolPropertyDefinition("scenarioIds", ToolDataType.ARRAY, "Optional list of scenario ids.", false))),
            new ToolSchemaDefinition(
                    "GoalScenarioComparisonResponse",
                    ToolDataType.OBJECT,
                    "Side-by-side scenario comparison result.",
                    List.of(new ToolPropertyDefinition("scenarios", ToolDataType.ARRAY, "Compared scenarios.", true))),
            CompareScenariosToolInput.class,
            GoalScenarioComparisonResponse.class);

    private final GoalSimulationService goalSimulationService;

    public CompareScenariosToolHandler(GoalSimulationService goalSimulationService) {
        this.goalSimulationService = goalSimulationService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(ToolExecutionContext context, CompareScenariosToolInput input) {
        GoalToolMappingSupport.requireGoalId(input == null ? null : input.goalId(), "goalId");
    }

    @Override
    public ToolResult<GoalScenarioComparisonResponse> doExecute(ToolExecutionContext context, CompareScenariosToolInput input) {
        try {
            return ToolResult.of(goalSimulationService.compareScenarios(
                    context.requireUserId(),
                    input.goalId(),
                    CompareScenariosRequest.builder().scenarioIds(input.scenarioIds()).build()));
        } catch (GoalNotFoundException | ScenarioNotFoundException ex) {
            throw GoalToolMappingSupport.notFound(ex.getMessage());
        }
    }
}
