package com.saveapenny.mcp.transaction;

import com.saveapenny.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RecentTransactionsToolResult(
        LocalDate from,
        LocalDate to,
        List<RecentTransactionItem> transactions) {

    public record RecentTransactionItem(
            UUID transactionId,
            UUID accountId,
            UUID categoryId,
            TransactionType type,
            BigDecimal amount,
            String currency,
            String description,
            LocalDate transactionDate) {
    }
}
