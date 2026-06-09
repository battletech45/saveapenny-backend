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
import com.saveapenny.mcp.execution.ToolValidationSupport;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ListGoalRunsToolHandler implements ToolHandler<ListGoalRunsToolInput, ListGoalRunsToolResult> {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "list_goal_runs",
            "List simulation runs for a goal owned by the authenticated user.",
            new ToolSchemaDefinition(
                    "ListGoalRunsToolInput",
                    ToolDataType.OBJECT,
                    "Input for listing goal runs.",
                    List.of(
                            new ToolPropertyDefinition("goalId", ToolDataType.STRING, "Goal id.", true),
                            new ToolPropertyDefinition("limit", ToolDataType.INTEGER, "Maximum number of runs to return.", false))),
            new ToolSchemaDefinition(
                    "ListGoalRunsToolResult",
                    ToolDataType.OBJECT,
                    "Run list for a goal.",
                    List.of(
                            new ToolPropertyDefinition("goalId", ToolDataType.STRING, "Goal id.", true),
                            new ToolPropertyDefinition("runs", ToolDataType.ARRAY, "Goal run items.", true))),
            ListGoalRunsToolInput.class,
            ListGoalRunsToolResult.class);

    private final GoalService goalService;

    public ListGoalRunsToolHandler(GoalService goalService) {
        this.goalService = goalService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(ToolExecutionContext context, ListGoalRunsToolInput input) {
        GoalToolMappingSupport.requireGoalId(input == null ? null : input.goalId(), "goalId");
        if (input != null) {
            ToolValidationSupport.requirePositiveInteger(input.limit(), "limit", "limit");
        }
    }

    @Override
    public ToolResult<ListGoalRunsToolResult> doExecute(ToolExecutionContext context, ListGoalRunsToolInput input) {
        try {
            return ToolResult.of(new ListGoalRunsToolResult(
                    input.goalId(),
                    goalService.listRuns(context.requireUserId(), input.goalId(), PageRequest.of(0, input.normalizedLimit()))
                            .getContent().stream()
                            .map(GoalToolMappingSupport::toGoalRunItem)
                            .toList()));
        } catch (GoalNotFoundException ex) {
            throw GoalToolMappingSupport.notFound(ex.getMessage());
        }
    }
}
