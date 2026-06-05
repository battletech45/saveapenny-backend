package com.saveapenny.mcp.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.saveapenny.budget.dto.BudgetStatusResponse;
import com.saveapenny.budget.entity.BudgetPeriod;
import com.saveapenny.budget.service.BudgetService;
import com.saveapenny.mcp.error.ToolValidationException;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class MonthlyBudgetStatusToolHandlerTest {

    @Mock
    private BudgetService budgetService;

    @Test
    void returnsStructuredBudgetStatus() {
        UUID userId = UUID.randomUUID();
        when(budgetService.getStatuses(userId, BudgetPeriod.MONTHLY, PageRequest.of(0, 3)))
                .thenReturn(new PageImpl<>(List.of(BudgetStatusResponse.builder()
                        .category("Food")
                        .budgetAmount(new BigDecimal("800.00"))
                        .spentAmount(new BigDecimal("500.00"))
                        .remainingAmount(new BigDecimal("300.00"))
                        .usagePercentage(new BigDecimal("62.50"))
                        .status("ON_TRACK")
                        .build())));

        MonthlyBudgetStatusToolHandler handler = new MonthlyBudgetStatusToolHandler(budgetService);

        MonthlyBudgetStatusToolResult result = handler.execute(new ToolExecutionContext(userId), new MonthlyBudgetStatusToolInput(3))
                .data();

        assertEquals(1, result.budgets().size());
        assertEquals("Food", result.budgets().getFirst().category());
        assertEquals("ON_TRACK", result.budgets().getFirst().status());
    }

    @Test
    void rejectsNonPositiveLimit() {
        MonthlyBudgetStatusToolHandler handler = new MonthlyBudgetStatusToolHandler(budgetService);

        assertThrows(
                ToolValidationException.class,
                () -> handler.execute(new ToolExecutionContext(UUID.randomUUID()), new MonthlyBudgetStatusToolInput(0)));
    }
}
