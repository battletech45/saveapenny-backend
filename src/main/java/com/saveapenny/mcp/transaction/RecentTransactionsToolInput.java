package com.saveapenny.mcp.transaction;

public record RecentTransactionsToolInput(Integer limit) {

    public int normalizedLimit() {
        return Math.max(1, limit == null ? 5 : limit);
    }
}
