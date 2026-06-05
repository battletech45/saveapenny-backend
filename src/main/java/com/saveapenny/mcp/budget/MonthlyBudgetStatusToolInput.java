package com.saveapenny.mcp.budget;

public record MonthlyBudgetStatusToolInput(Integer limit) {

    public int normalizedLimit() {
        return Math.max(1, limit == null ? 5 : limit);
    }
}
