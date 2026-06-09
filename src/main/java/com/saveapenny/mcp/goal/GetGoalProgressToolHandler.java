package com.saveapenny.mcp.goal;

import com.saveapenny.goal.exception.GoalNotFoundException;
import com.saveapenny.goal.service.GoalProgressCalculator;
import com.saveapenny.goal.service.GoalProgressReport;
import com.saveapenny.mcp.definition.ToolDataType;
import com.saveapenny.mcp.definition.ToolDefinition;
import com.saveapenny.mcp.definition.ToolPropertyDefinition;
import com.saveapenny.mcp.definition.ToolSchemaDefinition;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import com.saveapenny.mcp.execution.ToolHandler;
import com.saveapenny.mcp.execution.ToolResult;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GetGoalProgressToolHandler implements ToolHandler<GetGoalProgressToolInput, GetGoalProgressToolResult> {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "get_goal_progress",
            "Get a goal progress snapshot for the authenticated user.",
            new ToolSchemaDefinition(
                    "GetGoalProgressToolInput",
                    ToolDataType.OBJECT,
                    "Input for goal progress lookup.",
                    List.of(new ToolPropertyDefinition("goalId", ToolDataType.STRING, "Goal id.", true))),
            new ToolSchemaDefinition(
                    "GetGoalProgressToolResult",
                    ToolDataType.OBJECT,
                    "Current goal progress snapshot.",
                    List.of(
                            new ToolPropertyDefinition("goalId", ToolDataType.STRING, "Goal id.", true),
                            new ToolPropertyDefinition("baselineRunId", ToolDataType.STRING, "Baseline run id when available.", false),
                            new ToolPropertyDefinition("status", ToolDataType.STRING, "Progress status.", true),
                            new ToolPropertyDefinition("offTrackForMonthsCount", ToolDataType.INTEGER, "Consecutive off-track observations.", false),
                            new ToolPropertyDefinition("warnings", ToolDataType.ARRAY, "Progress warnings.", true))),
            GetGoalProgressToolInput.class,
            GetGoalProgressToolResult.class);

    private final GoalProgressCalculator goalProgressCalculator;
    private final Clock assistantClock;

    public GetGoalProgressToolHandler(GoalProgressCalculator goalProgressCalculator, Clock assistantClock) {
        this.goalProgressCalculator = goalProgressCalculator;
        this.assistantClock = assistantClock;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(ToolExecutionContext context, GetGoalProgressToolInput input) {
        GoalToolMappingSupport.requireGoalId(input == null ? null : input.goalId(), "goalId");
    }

    @Override
    public ToolResult<GetGoalProgressToolResult> doExecute(ToolExecutionContext context, GetGoalProgressToolInput input) {
        try {
            GoalProgressReport report = goalProgressCalculator.calculate(
                    context.requireUserId(),
                    input.goalId(),
                    LocalDate.now(assistantClock));
            return ToolResult.of(new GetGoalProgressToolResult(
                    report.goalId(),
                    report.baselineRunId(),
                    report.currentAmount(),
                    report.projectedAmountAtTarget(),
                    report.gap(),
                    report.monthsRemaining(),
                    report.offTrackForMonthsCount(),
                    mapStatus(report.status()),
                    report.warnings().stream().map(item -> new GoalToolModels.GoalWarning(item.code(), item.message())).toList()));
        } catch (GoalNotFoundException ex) {
            throw GoalToolMappingSupport.notFound(ex.getMessage());
        }
    }

    private GoalToolModels.ProgressStatus mapStatus(GoalProgressReport.ProgressStatus status) {
        return switch (status) {
            case ON_TRACK -> GoalToolModels.ProgressStatus.ON_TRACK;
            case AT_RISK -> GoalToolModels.ProgressStatus.AT_RISK;
            case OFF_TRACK -> GoalToolModels.ProgressStatus.OFF_TRACK;
            case ACHIEVED -> GoalToolModels.ProgressStatus.ACHIEVED;
            case NO_PROJECTION -> GoalToolModels.ProgressStatus.NO_PROJECTION;
        };
    }
}
