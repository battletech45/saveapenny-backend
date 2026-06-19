package com.saveapenny.mcp.adapter.springai;

import com.saveapenny.assistant.tool.AssistantToolContextHolder;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.mcp.error.ToolError;
import com.saveapenny.mcp.error.ToolValidationException;
import com.saveapenny.mcp.budget.MonthlyBudgetStatusToolInput;
import com.saveapenny.mcp.budget.MonthlyBudgetStatusToolResult;
import com.saveapenny.mcp.goal.CompareScenariosToolInput;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import com.saveapenny.mcp.execution.ToolHandler;
import com.saveapenny.mcp.execution.ToolResult;
import com.saveapenny.mcp.goal.GetGoalProgressToolInput;
import com.saveapenny.mcp.goal.GetGoalProgressToolResult;
import com.saveapenny.mcp.goal.GetGoalToolInput;
import com.saveapenny.mcp.goal.GetGoalToolResult;
import com.saveapenny.mcp.goal.GoalToolModels;
import com.saveapenny.mcp.goal.ListGoalRunsToolInput;
import com.saveapenny.mcp.goal.ListGoalRunsToolResult;
import com.saveapenny.mcp.goal.ListGoalScenariosToolInput;
import com.saveapenny.mcp.goal.ListGoalScenariosToolResult;
import com.saveapenny.mcp.goal.ListGoalsToolInput;
import com.saveapenny.mcp.goal.ListGoalsToolResult;
import com.saveapenny.mcp.goal.SimulateGoalToolInput;
import com.saveapenny.mcp.goal.WhatIfToolInput;
import com.saveapenny.mcp.registry.ToolRegistry;
import com.saveapenny.mcp.report.CurrentMonthSummaryToolInput;
import com.saveapenny.mcp.report.CurrentMonthSummaryToolResult;
import com.saveapenny.mcp.report.TopSpendingCategoriesToolInput;
import com.saveapenny.mcp.report.TopSpendingCategoriesToolResult;
import com.saveapenny.mcp.transaction.RecentTransactionsToolInput;
import com.saveapenny.mcp.transaction.RecentTransactionsToolResult;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class SpringAiMcpToolAdapter {

    private static final String CURRENT_MONTH_SUMMARY_TOOL = "getCurrentMonthSummary";
    private static final String TOP_SPENDING_CATEGORIES_TOOL = "getTopSpendingCategories";
    private static final String MONTHLY_BUDGET_STATUS_TOOL = "getMonthlyBudgetStatus";
    private static final String RECENT_TRANSACTIONS_TOOL = "getRecentTransactions";
    private static final String LIST_GOALS_TOOL = "list_goals";
    private static final String GET_GOAL_TOOL = "get_goal";
    private static final String GET_GOAL_PROGRESS_TOOL = "get_goal_progress";
    private static final String LIST_GOAL_SCENARIOS_TOOL = "list_goal_scenarios";
    private static final String LIST_GOAL_RUNS_TOOL = "list_goal_runs";
    private static final String SIMULATE_GOAL_TOOL = "simulate_goal";
    private static final String COMPARE_SCENARIOS_TOOL = "compare_scenarios";
    private static final String WHAT_IF_TOOL = "what_if";

    private final ToolRegistry toolRegistry;
    private final AssistantToolContextHolder assistantToolContextHolder;

    public SpringAiMcpToolAdapter(
            ToolRegistry toolRegistry,
            AssistantToolContextHolder assistantToolContextHolder) {
        this.toolRegistry = toolRegistry;
        this.assistantToolContextHolder = assistantToolContextHolder;
    }

    @Tool(name = CURRENT_MONTH_SUMMARY_TOOL, description = "Get the authenticated user's current month income, expense, and net savings summary.")
    public String getCurrentMonthSummary() {
        CurrentMonthSummaryToolResult summary = execute(
                CURRENT_MONTH_SUMMARY_TOOL,
                new CurrentMonthSummaryToolInput(),
                CurrentMonthSummaryToolResult.class);
        return "Current month summary: income="
                + summary.totalIncome()
                + ", expense="
                + summary.totalExpense()
                + ", netSavings="
                + summary.netSavings()
                + '.';
    }

    @Tool(name = TOP_SPENDING_CATEGORIES_TOOL, description = "Get the authenticated user's top expense categories for the current month.")
    public String getTopSpendingCategories(
            @ToolParam(description = "Maximum number of categories to return.", required = false) Integer topCategoriesLimit) {
        TopSpendingCategoriesToolResult topCategories = execute(
                         TOP_SPENDING_CATEGORIES_TOOL,
                         new TopSpendingCategoriesToolInput(topCategoriesLimit),
                        TopSpendingCategoriesToolResult.class);

        if (topCategories.categories().isEmpty()) {
            return "Top spending categories: none for the current month.";
        }

        StringBuilder builder = new StringBuilder("Top spending categories: ");
        for (int i = 0; i < topCategories.categories().size(); i++) {
            TopSpendingCategoriesToolResult.TopSpendingCategoryItem item = topCategories.categories().get(i);
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(item.categoryName())
                    .append('=')
                    .append(item.totalAmount())
                    .append(" (")
                    .append(item.usagePercentage())
                    .append("%)");
        }
        builder.append('.');
        return builder.toString();
    }

    @Tool(name = MONTHLY_BUDGET_STATUS_TOOL, description = "Get the authenticated user's monthly budget status overview.")
    public String getMonthlyBudgetStatus(
            @ToolParam(description = "Maximum number of budget entries to include.", required = false) Integer limit) {
        MonthlyBudgetStatusToolResult budgets = execute(
                MONTHLY_BUDGET_STATUS_TOOL,
                new MonthlyBudgetStatusToolInput(limit),
                MonthlyBudgetStatusToolResult.class);

        if (budgets.budgets().isEmpty()) {
            return "Monthly budget status: none.";
        }

        StringBuilder builder = new StringBuilder("Monthly budget status: ");
        for (int i = 0; i < budgets.budgets().size(); i++) {
            MonthlyBudgetStatusToolResult.BudgetStatusItem status = budgets.budgets().get(i);
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(status.category())
                    .append(" spent=")
                    .append(status.spentAmount())
                    .append(" of ")
                    .append(status.budgetAmount())
                    .append(", remaining=")
                    .append(status.remainingAmount())
                    .append(", status=")
                    .append(status.status());
        }
        builder.append('.');
        return builder.toString();
    }

    @Tool(name = RECENT_TRANSACTIONS_TOOL, description = "Get the authenticated user's recent transactions from the last 30 days.")
    public String getRecentTransactions(
            @ToolParam(description = "Maximum number of transactions to include.", required = false) Integer limit) {
        RecentTransactionsToolResult transactions = execute(
                RECENT_TRANSACTIONS_TOOL,
                new RecentTransactionsToolInput(limit),
                RecentTransactionsToolResult.class);
        if (transactions.transactions().isEmpty()) {
            return "Recent transactions: none in the last 30 days.";
        }

        StringBuilder builder = new StringBuilder("Recent transactions: ");
        for (int i = 0; i < transactions.transactions().size(); i++) {
            RecentTransactionsToolResult.RecentTransactionItem item = transactions.transactions().get(i);
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(item.transactionDate())
                    .append(' ')
                    .append(item.type())
                    .append(' ')
                    .append(item.amount());
            if (item.description() != null && !item.description().isBlank()) {
                builder.append(" (")
                        .append(item.description().trim())
                        .append(')');
            }
        }
        builder.append('.');
        return builder.toString();
    }

    @Tool(name = LIST_GOALS_TOOL, description = "List the authenticated user's goals with optional status and type filters.")
    public String listGoals(
            @ToolParam(description = "Optional status filter.", required = false) String status,
            @ToolParam(description = "Optional goal type filter.", required = false) String type,
            @ToolParam(description = "Maximum number of goals to include.", required = false) Integer limit) {
        ListGoalsToolResult result = execute(
                LIST_GOALS_TOOL,
                new ListGoalsToolInput(parseGoalStatus(status), parseGoalType(type), limit),
                ListGoalsToolResult.class);
        if (result.goals().isEmpty()) {
            return "Goals: none.";
        }
        StringBuilder builder = new StringBuilder("Goals: ");
        for (int i = 0; i < result.goals().size(); i++) {
            var item = result.goals().get(i);
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(item.title())
                    .append(" [")
                    .append(item.type())
                    .append(", ")
                    .append(item.status())
                    .append("] target=")
                    .append(item.targetAmount())
                    .append(' ')
                    .append(item.currency());
        }
        builder.append('.');
        return builder.toString();
    }

    @Tool(name = GET_GOAL_TOOL, description = "Get one goal with scenarios and latest run.")
    public String getGoal(@ToolParam(description = "Goal id.") String goalId) {
        GetGoalToolResult result = execute(GET_GOAL_TOOL, new GetGoalToolInput(parseUuid(goalId, "goalId")), GetGoalToolResult.class);
        var goal = result.goal();
        return "Goal: " + goal.title() + " [" + goal.type() + ", " + goal.status() + "] target="
                + goal.targetAmount() + ' ' + goal.currency()
                + ", scenarios=" + goal.scenarios().size()
                + ", latestRun=" + (goal.latestRun() == null ? "none" : goal.latestRun().feasibility()) + '.';
    }

    @Tool(name = GET_GOAL_PROGRESS_TOOL, description = "Get the authenticated user's progress snapshot for a goal.")
    public String getGoalProgress(@ToolParam(description = "Goal id.") String goalId) {
        GetGoalProgressToolResult result = execute(
                GET_GOAL_PROGRESS_TOOL,
                new GetGoalProgressToolInput(parseUuid(goalId, "goalId")),
                GetGoalProgressToolResult.class);
        return "Goal progress: status=" + result.status()
                + ", currentAmount=" + result.currentAmount()
                + ", projectedAmountAtTarget=" + result.projectedAmountAtTarget()
                + ", gap=" + result.gap()
                + ", offTrackForMonthsCount=" + result.offTrackForMonthsCount() + '.';
    }

    @Tool(name = LIST_GOAL_SCENARIOS_TOOL, description = "List scenarios for a goal.")
    public String listGoalScenarios(@ToolParam(description = "Goal id.") String goalId) {
        ListGoalScenariosToolResult result = execute(
                LIST_GOAL_SCENARIOS_TOOL,
                new ListGoalScenariosToolInput(parseUuid(goalId, "goalId")),
                ListGoalScenariosToolResult.class);
        if (result.scenarios().isEmpty()) {
            return "Goal scenarios: none.";
        }
        StringBuilder builder = new StringBuilder("Goal scenarios: ");
        for (int i = 0; i < result.scenarios().size(); i++) {
            var scenario = result.scenarios().get(i);
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(scenario.name());
            if (scenario.isBaseline()) {
                builder.append(" [baseline]");
            }
        }
        builder.append('.');
        return builder.toString();
    }

    @Tool(name = LIST_GOAL_RUNS_TOOL, description = "List simulation runs for a goal.")
    public String listGoalRuns(
            @ToolParam(description = "Goal id.") String goalId,
            @ToolParam(description = "Maximum number of runs to include.", required = false) Integer limit) {
        ListGoalRunsToolResult result = execute(
                LIST_GOAL_RUNS_TOOL,
                new ListGoalRunsToolInput(parseUuid(goalId, "goalId"), limit),
                ListGoalRunsToolResult.class);
        if (result.runs().isEmpty()) {
            return "Goal runs: none.";
        }
        StringBuilder builder = new StringBuilder("Goal runs: ");
        for (int i = 0; i < result.runs().size(); i++) {
            var run = result.runs().get(i);
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(run.runId()).append(" [").append(run.feasibility()).append("]");
        }
        builder.append('.');
        return builder.toString();
    }

    @Tool(name = SIMULATE_GOAL_TOOL, description = "Run a live simulation for an existing goal.")
    public String simulateGoal(@ToolParam(description = "Goal id.") String goalId) {
        var result = execute(
                SIMULATE_GOAL_TOOL,
                new SimulateGoalToolInput(parseUuid(goalId, "goalId"), null),
                com.saveapenny.goal.simulation.SimulationResult.class);
        return "Goal simulation: feasibility=" + result.getFeasibility()
                + ", horizonMonths=" + result.getHorizonMonths()
                + ", warnings=" + result.getWarnings().size() + '.';
    }

    @Tool(name = COMPARE_SCENARIOS_TOOL, description = "Compare scenarios for a goal.")
    public String compareScenarios(@ToolParam(description = "Goal id.") String goalId) {
        var result = execute(
                COMPARE_SCENARIOS_TOOL,
                new CompareScenariosToolInput(parseUuid(goalId, "goalId"), null),
                com.saveapenny.goal.simulation.dto.GoalScenarioComparisonResponse.class);
        if (result.getScenarios().isEmpty()) {
            return "Scenario comparison: none.";
        }
        StringBuilder builder = new StringBuilder("Scenario comparison: ");
        for (int i = 0; i < result.getScenarios().size(); i++) {
            var scenario = result.getScenarios().get(i);
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(scenario.getScenarioName())
                    .append(" feasibility=")
                    .append(scenario.getFeasibility())
                    .append(" projected=")
                    .append(scenario.getProjectedAmount());
        }
        builder.append('.');
        return builder.toString();
    }

    @Tool(name = WHAT_IF_TOOL, description = "Run a one-off what-if simulation for a goal.")
    public String whatIf(
            @ToolParam(description = "Goal id.") String goalId,
            @ToolParam(description = "New monthly contribution for savings-style goals.", required = false) Double monthlyContribution) {
        var overrides = JsonNodeFactory.instance.objectNode();
        if (monthlyContribution != null) {
            overrides.put("monthlyContribution", monthlyContribution);
        }
        var result = execute(
                WHAT_IF_TOOL,
                new WhatIfToolInput(parseUuid(goalId, "goalId"), overrides),
                com.saveapenny.goal.simulation.dto.GoalWhatIfResponse.class);
        return "What-if result: feasibility=" + result.getResult().getFeasibility()
                + ", projected=" + result.getResult().getSummary().get("projectedAmount")
                + ", deltaVsBaseline=" + (result.getDeltaVsBaseline() == null ? "none" : result.getDeltaVsBaseline().getProjectedAmountDelta()) + '.';
    }

    @SuppressWarnings("unchecked")
    private <I, O> O execute(String toolName, I input, Class<O> outputType) {
        ToolHandler<I, O> handler = (ToolHandler<I, O>) toolRegistry.findByName(toolName)
                .orElseThrow(() -> new IllegalStateException("Spring AI MCP adapter could not resolve tool: " + toolName));
        ToolResult<O> result = handler.execute(currentContext(), input);
        return outputType.cast(result.data());
    }

    private ToolExecutionContext currentContext() {
        return new ToolExecutionContext(assistantToolContextHolder.requireCurrentUserId());
    }

    private GoalStatus parseGoalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return GoalStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw invalidEnum("status", status, GoalStatus.values());
        }
    }

    private GoalType parseGoalType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return GoalType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw invalidEnum("type", type, GoalType.values());
        }
    }

    private UUID parseUuid(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new ToolValidationException(
                    fieldName + " must be a valid UUID.",
                    java.util.List.of(new ToolError(null, fieldName + " must be a valid UUID.", fieldName)));
        }
    }

    private <E extends Enum<E>> ToolValidationException invalidEnum(String fieldName, String value, E[] allowedValues) {
        return new ToolValidationException(
                fieldName + " must be one of: " + java.util.Arrays.toString(allowedValues) + '.',
                java.util.List.of(new ToolError(
                        null,
                        fieldName + " value '" + value + "' is invalid.",
                        fieldName)));
    }
}
