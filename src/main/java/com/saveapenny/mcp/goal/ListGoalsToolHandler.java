package com.saveapenny.mcp.goal;

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
public class ListGoalsToolHandler implements ToolHandler<ListGoalsToolInput, ListGoalsToolResult> {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "list_goals",
            "List the authenticated user's goals with optional type and status filters.",
            new ToolSchemaDefinition(
                    "ListGoalsToolInput",
                    ToolDataType.OBJECT,
                    "Input for goal listing.",
                    List.of(
                            new ToolPropertyDefinition("status", ToolDataType.STRING, "Optional goal status filter.", false),
                            new ToolPropertyDefinition("type", ToolDataType.STRING, "Optional goal type filter.", false),
                            new ToolPropertyDefinition("limit", ToolDataType.INTEGER, "Maximum number of goals to return.", false))),
            new ToolSchemaDefinition(
                    "ListGoalsToolResult",
                    ToolDataType.OBJECT,
                    "Paginated goal list for the current user.",
                    List.of(new ToolPropertyDefinition("goals", ToolDataType.ARRAY, "Goal items.", true))),
            ListGoalsToolInput.class,
            ListGoalsToolResult.class);

    private final GoalService goalService;

    public ListGoalsToolHandler(GoalService goalService) {
        this.goalService = goalService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(ToolExecutionContext context, ListGoalsToolInput input) {
        if (input != null) {
            ToolValidationSupport.requirePositiveInteger(input.limit(), "limit", "limit");
        }
    }

    @Override
    public ToolResult<ListGoalsToolResult> doExecute(ToolExecutionContext context, ListGoalsToolInput input) {
        List<GoalToolModels.GoalItem> goals = goalService.getAll(
                        context.requireUserId(),
                        input == null ? null : input.status(),
                        input == null ? null : input.type(),
                        PageRequest.of(0, input == null ? 10 : input.normalizedLimit()))
                .getContent().stream()
                .map(GoalToolMappingSupport::toGoalItem)
                .toList();
        return ToolResult.of(new ListGoalsToolResult(goals));
    }
}
