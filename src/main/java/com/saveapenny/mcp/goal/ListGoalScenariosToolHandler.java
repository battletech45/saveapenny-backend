package com.saveapenny.mcp.goal;

import com.saveapenny.goal.exception.GoalNotFoundException;
import com.saveapenny.goal.service.GoalService;
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
public class ListGoalScenariosToolHandler implements ToolHandler<ListGoalScenariosToolInput, ListGoalScenariosToolResult> {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "list_goal_scenarios",
            "List scenarios for a goal owned by the authenticated user.",
            new ToolSchemaDefinition(
                    "ListGoalScenariosToolInput",
                    ToolDataType.OBJECT,
                    "Input for listing goal scenarios.",
                    List.of(new ToolPropertyDefinition("goalId", ToolDataType.STRING, "Goal id.", true))),
            new ToolSchemaDefinition(
                    "ListGoalScenariosToolResult",
                    ToolDataType.OBJECT,
                    "Scenario list for a goal.",
                    List.of(
                            new ToolPropertyDefinition("goalId", ToolDataType.STRING, "Goal id.", true),
                            new ToolPropertyDefinition("scenarios", ToolDataType.ARRAY, "Scenario items.", true))),
            ListGoalScenariosToolInput.class,
            ListGoalScenariosToolResult.class);

    private final GoalService goalService;

    public ListGoalScenariosToolHandler(GoalService goalService) {
        this.goalService = goalService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(ToolExecutionContext context, ListGoalScenariosToolInput input) {
        GoalToolMappingSupport.requireGoalId(input == null ? null : input.goalId(), "goalId");
    }

    @Override
    public ToolResult<ListGoalScenariosToolResult> doExecute(ToolExecutionContext context, ListGoalScenariosToolInput input) {
        try {
            return ToolResult.of(new ListGoalScenariosToolResult(
                    input.goalId(),
                    goalService.listScenarios(context.requireUserId(), input.goalId()).stream()
                            .map(GoalToolMappingSupport::toScenarioItem)
                            .toList()));
        } catch (GoalNotFoundException ex) {
            throw GoalToolMappingSupport.notFound(ex.getMessage());
        }
    }
}
