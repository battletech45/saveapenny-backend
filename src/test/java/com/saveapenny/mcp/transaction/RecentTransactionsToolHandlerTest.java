package com.saveapenny.mcp.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.saveapenny.mcp.error.ToolValidationException;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class RecentTransactionsToolHandlerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-18T10:15:30Z"), ZoneOffset.UTC);

    @Mock
    private TransactionService transactionService;

    @Test
    void returnsStructuredRecentTransactions() {
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 5, 18);
        when(transactionService.getAll(
                        eq(userId),
                        eq(today.minusDays(30)),
                        eq(today),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(null),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(TransactionResponse.builder()
                        .id(UUID.randomUUID())
                        .accountId(UUID.randomUUID())
                        .categoryId(UUID.randomUUID())
                        .type(TransactionType.EXPENSE)
                        .amount(new BigDecimal("45.00"))
                        .currency("TRY")
                        .description("Coffee")
                        .transactionDate(today.minusDays(1))
                        .build())));

        RecentTransactionsToolHandler handler = new RecentTransactionsToolHandler(transactionService, FIXED_CLOCK);

        RecentTransactionsToolResult result = handler.execute(new ToolExecutionContext(userId), new RecentTransactionsToolInput(5))
                .data();

        assertEquals(1, result.transactions().size());
        assertEquals(TransactionType.EXPENSE, result.transactions().getFirst().type());
        assertEquals("Coffee", result.transactions().getFirst().description());
    }

    @Test
    void rejectsNonPositiveLimit() {
        RecentTransactionsToolHandler handler = new RecentTransactionsToolHandler(transactionService, FIXED_CLOCK);

        assertThrows(
                ToolValidationException.class,
                () -> handler.execute(new ToolExecutionContext(UUID.randomUUID()), new RecentTransactionsToolInput(0)));
    }
}
