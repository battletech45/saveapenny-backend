package com.saveapenny.mcp.budget;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyBudgetStatusToolResult(List<BudgetStatusItem> budgets) {

    public record BudgetStatusItem(
            String category,
            BigDecimal budgetAmount,
            BigDecimal spentAmount,
            BigDecimal remainingAmount,
            BigDecimal usagePercentage,
            String status) {
    }
}
