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
public class GetGoalToolHandler implements ToolHandler<GetGoalToolInput, GetGoalToolResult> {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "get_goal",
            "Get a single goal with its scenarios and latest run.",
            new ToolSchemaDefinition(
                    "GetGoalToolInput",
                    ToolDataType.OBJECT,
                    "Input for goal detail retrieval.",
                    List.of(new ToolPropertyDefinition("goalId", ToolDataType.STRING, "Goal id.", true))),
            new ToolSchemaDefinition(
                    "GetGoalToolResult",
                    ToolDataType.OBJECT,
                    "Goal detail for the current user.",
                    List.of(new ToolPropertyDefinition("goal", ToolDataType.OBJECT, "Goal detail item.", true))),
            GetGoalToolInput.class,
            GetGoalToolResult.class);

    private final GoalService goalService;

    public GetGoalToolHandler(GoalService goalService) {
        this.goalService = goalService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(ToolExecutionContext context, GetGoalToolInput input) {
        GoalToolMappingSupport.requireGoalId(input == null ? null : input.goalId(), "goalId");
    }

    @Override
    public ToolResult<GetGoalToolResult> doExecute(ToolExecutionContext context, GetGoalToolInput input) {
        try {
            return ToolResult.of(new GetGoalToolResult(
                    GoalToolMappingSupport.toGoalDetailItem(goalService.getById(context.requireUserId(), input.goalId()))));
        } catch (GoalNotFoundException ex) {
            throw GoalToolMappingSupport.notFound(ex.getMessage());
        }
    }
}
