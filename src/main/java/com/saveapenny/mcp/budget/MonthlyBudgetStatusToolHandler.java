package com.saveapenny.mcp.budget;

import com.saveapenny.budget.dto.BudgetStatusResponse;
import com.saveapenny.budget.entity.BudgetPeriod;
import com.saveapenny.budget.service.BudgetService;
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
public class MonthlyBudgetStatusToolHandler implements ToolHandler<MonthlyBudgetStatusToolInput, MonthlyBudgetStatusToolResult> {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "getMonthlyBudgetStatus",
            "Get the authenticated user's monthly budget status overview.",
            new ToolSchemaDefinition(
                    "MonthlyBudgetStatusToolInput",
                    ToolDataType.OBJECT,
                    "Input for monthly budget status retrieval.",
                    List.of(new ToolPropertyDefinition("limit", ToolDataType.INTEGER, "Maximum number of budget entries to return.", false))),
            new ToolSchemaDefinition(
                    "MonthlyBudgetStatusToolResult",
                    ToolDataType.OBJECT,
                    "Budget status results for the current user.",
                    List.of(new ToolPropertyDefinition("budgets", ToolDataType.ARRAY, "Monthly budget entries.", true))),
            MonthlyBudgetStatusToolInput.class,
            MonthlyBudgetStatusToolResult.class);

    private final BudgetService budgetService;

    public MonthlyBudgetStatusToolHandler(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(ToolExecutionContext context, MonthlyBudgetStatusToolInput input) {
        if (input != null) {
            ToolValidationSupport.requirePositiveInteger(input.limit(), "limit", "limit");
        }
    }

    @Override
    public ToolResult<MonthlyBudgetStatusToolResult> doExecute(
            ToolExecutionContext context,
            MonthlyBudgetStatusToolInput input) {
        List<MonthlyBudgetStatusToolResult.BudgetStatusItem> budgets = budgetService.getStatuses(
                        context.requireUserId(),
                        BudgetPeriod.MONTHLY,
                        PageRequest.of(0, input == null ? 5 : input.normalizedLimit()))
                .getContent()
                .stream()
                .map(this::toItem)
                .toList();
        return ToolResult.of(new MonthlyBudgetStatusToolResult(budgets));
    }

    private MonthlyBudgetStatusToolResult.BudgetStatusItem toItem(BudgetStatusResponse status) {
        return new MonthlyBudgetStatusToolResult.BudgetStatusItem(
                status.getCategory(),
                status.getBudgetAmount(),
                status.getSpentAmount(),
                status.getRemainingAmount(),
                status.getUsagePercentage(),
                status.getStatus());
    }
}
