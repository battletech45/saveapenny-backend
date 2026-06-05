package com.saveapenny.mcp.transaction;

import com.saveapenny.mcp.definition.ToolDataType;
import com.saveapenny.mcp.definition.ToolDefinition;
import com.saveapenny.mcp.definition.ToolPropertyDefinition;
import com.saveapenny.mcp.definition.ToolSchemaDefinition;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import com.saveapenny.mcp.execution.ToolHandler;
import com.saveapenny.mcp.execution.ToolResult;
import com.saveapenny.mcp.execution.ToolValidationSupport;
import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.service.TransactionService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class RecentTransactionsToolHandler implements ToolHandler<RecentTransactionsToolInput, RecentTransactionsToolResult> {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "getRecentTransactions",
            "Get the authenticated user's recent transactions from the last 30 days.",
            new ToolSchemaDefinition(
                    "RecentTransactionsToolInput",
                    ToolDataType.OBJECT,
                    "Input for recent transaction lookup.",
                    List.of(new ToolPropertyDefinition("limit", ToolDataType.INTEGER, "Maximum number of transactions to return.", false))),
            new ToolSchemaDefinition(
                    "RecentTransactionsToolResult",
                    ToolDataType.OBJECT,
                    "Recent transactions for the current user.",
                    List.of(
                            new ToolPropertyDefinition("from", ToolDataType.STRING, "Transaction range start date.", true),
                            new ToolPropertyDefinition("to", ToolDataType.STRING, "Transaction range end date.", true),
                            new ToolPropertyDefinition("transactions", ToolDataType.ARRAY, "Recent transaction list.", true))),
            RecentTransactionsToolInput.class,
            RecentTransactionsToolResult.class);

    private final TransactionService transactionService;
    private final Clock clock;

    public RecentTransactionsToolHandler(TransactionService transactionService, Clock clock) {
        this.transactionService = transactionService;
        this.clock = clock;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(ToolExecutionContext context, RecentTransactionsToolInput input) {
        if (input != null) {
            ToolValidationSupport.requirePositiveInteger(input.limit(), "limit", "limit");
        }
    }

    @Override
    public ToolResult<RecentTransactionsToolResult> doExecute(
            ToolExecutionContext context,
            RecentTransactionsToolInput input) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(30);
        List<RecentTransactionsToolResult.RecentTransactionItem> transactions = transactionService.getAll(
                        context.requireUserId(),
                        from,
                        today,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(
                                0,
                                input == null ? 5 : input.normalizedLimit(),
                                Sort.by(Sort.Direction.DESC, "transactionDate", "createdAt")))
                .getContent()
                .stream()
                .map(this::toItem)
                .toList();
        return ToolResult.of(new RecentTransactionsToolResult(from, today, transactions));
    }

    private RecentTransactionsToolResult.RecentTransactionItem toItem(TransactionResponse item) {
        return new RecentTransactionsToolResult.RecentTransactionItem(
                item.getId(),
                item.getAccountId(),
                item.getCategoryId(),
                item.getType(),
                item.getAmount(),
                item.getCurrency(),
                item.getDescription(),
                item.getTransactionDate());
    }
}
