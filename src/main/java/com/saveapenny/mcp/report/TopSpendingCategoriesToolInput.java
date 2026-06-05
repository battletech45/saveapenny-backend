package com.saveapenny.mcp.report;

public record TopSpendingCategoriesToolInput(Integer limit) {

    public int normalizedLimit() {
        return Math.max(1, limit == null ? 5 : limit);
    }
}
