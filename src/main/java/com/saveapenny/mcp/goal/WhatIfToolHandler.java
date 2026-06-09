package com.saveapenny.mcp.goal;

import com.saveapenny.goal.exception.GoalNotFoundException;
import com.saveapenny.goal.service.GoalSimulationService;
import com.saveapenny.goal.simulation.dto.GoalWhatIfResponse;
import com.saveapenny.goal.simulation.dto.WhatIfRequest;
import com.saveapenny.mcp.definition.ToolDataType;
import com.saveapenny.mcp.definition.ToolDefinition;
import com.saveapenny.mcp.definition.ToolPropertyDefinition;
import com.saveapenny.mcp.definition.ToolSchemaDefinition;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import com.saveapenny.mcp.execution.ToolHandler;
import com.saveapenny.mcp.execution.ToolResult;
import com.saveapenny.mcp.error.ToolError;
import com.saveapenny.mcp.error.ToolValidationException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WhatIfToolHandler implements ToolHandler<WhatIfToolInput, GoalWhatIfResponse> {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "what_if",
            "Run a one-off projection for a goal with flat input overrides and no persistence.",
            new ToolSchemaDefinition(
                    "WhatIfToolInput",
                    ToolDataType.OBJECT,
                    "Input for a what-if simulation.",
                    List.of(
                            new ToolPropertyDefinition("goalId", ToolDataType.STRING, "Goal id.", true),
                            new ToolPropertyDefinition("overrides", ToolDataType.OBJECT, "Flat override object.", true))),
            new ToolSchemaDefinition(
                    "GoalWhatIfResponse",
                    ToolDataType.OBJECT,
                    "Projection result for a what-if request.",
                    List.of(new ToolPropertyDefinition("result", ToolDataType.OBJECT, "Simulation result.", true))),
            WhatIfToolInput.class,
            GoalWhatIfResponse.class);

    private final GoalSimulationService goalSimulationService;

    public WhatIfToolHandler(GoalSimulationService goalSimulationService) {
        this.goalSimulationService = goalSimulationService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(ToolExecutionContext context, WhatIfToolInput input) {
        GoalToolMappingSupport.requireGoalId(input == null ? null : input.goalId(), "goalId");
        if (input == null || input.overrides() == null || !input.overrides().isObject()) {
            throw new ToolValidationException(
                    "overrides is required and must be an object.",
                    List.of(new ToolError(null, "overrides is required and must be an object.", "overrides")));
        }
    }

    @Override
    public ToolResult<GoalWhatIfResponse> doExecute(ToolExecutionContext context, WhatIfToolInput input) {
        try {
            return ToolResult.of(goalSimulationService.whatIf(
                    context.requireUserId(),
                    input.goalId(),
                    WhatIfRequest.builder().overrides(input.overrides()).build()));
        } catch (GoalNotFoundException ex) {
            throw GoalToolMappingSupport.notFound(ex.getMessage());
        }
    }
}
