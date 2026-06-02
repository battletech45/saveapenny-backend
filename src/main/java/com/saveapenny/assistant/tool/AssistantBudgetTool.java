package com.saveapenny.assistant.tool;

import com.saveapenny.budget.dto.BudgetResponse;
import com.saveapenny.budget.dto.BudgetStatusResponse;
import com.saveapenny.budget.entity.BudgetPeriod;
import com.saveapenny.budget.service.BudgetService;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class AssistantBudgetTool {

    private final BudgetService budgetService;
    private final AssistantToolContextHolder assistantToolContextHolder;

    public AssistantBudgetTool(BudgetService budgetService, AssistantToolContextHolder assistantToolContextHolder) {
        this.budgetService = budgetService;
        this.assistantToolContextHolder = assistantToolContextHolder;
    }

    @Tool(name = "getMonthlyBudgetStatus", description = "Get the authenticated user's monthly budget status overview.")
    public String getMonthlyBudgetStatus(
            @ToolParam(description = "Maximum number of budget entries to include.", required = false) int limit) {
        UUID userId = assistantToolContextHolder.requireCurrentUserId();
        Page<BudgetResponse> budgets = budgetService.getAll(
                userId,
                BudgetPeriod.MONTHLY,
                PageRequest.of(0, Math.max(1, limit)));

        if (budgets.isEmpty()) {
            return "Monthly budget status: none.";
        }

        StringBuilder builder = new StringBuilder("Monthly budget status: ");
        for (int i = 0; i < budgets.getContent().size(); i++) {
            BudgetStatusResponse status = budgetService.getStatus(userId, budgets.getContent().get(i).getId());
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(status.getCategory())
                    .append(" spent=")
                    .append(status.getSpentAmount())
                    .append(" of ")
                    .append(status.getBudgetAmount())
                    .append(", remaining=")
                    .append(status.getRemainingAmount())
                    .append(", status=")
                    .append(status.getStatus());
        }
        builder.append('.');
        return builder.toString();
    }
}
