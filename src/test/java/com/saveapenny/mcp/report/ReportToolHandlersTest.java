package com.saveapenny.mcp.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.saveapenny.mcp.error.ToolValidationException;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import com.saveapenny.report.dto.CategorySpendingResponse;
import com.saveapenny.report.dto.MonthlySummaryResponse;
import com.saveapenny.report.service.ReportService;
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

@ExtendWith(MockitoExtension.class)
class ReportToolHandlersTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-18T10:15:30Z"), ZoneOffset.UTC);

    @Mock
    private ReportService reportService;

    @Test
    void currentMonthSummary_returnsStructuredSummary() {
        UUID userId = UUID.randomUUID();
        when(reportService.getMonthlySummary(userId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 18)))
                .thenReturn(MonthlySummaryResponse.builder()
                        .startDate(LocalDate.of(2026, 5, 1))
                        .endDate(LocalDate.of(2026, 5, 18))
                        .totalIncome(new BigDecimal("3000.00"))
                        .totalExpense(new BigDecimal("1800.00"))
                        .netSavings(new BigDecimal("1200.00"))
                        .build());

        CurrentMonthSummaryToolHandler handler = new CurrentMonthSummaryToolHandler(reportService, FIXED_CLOCK);

        CurrentMonthSummaryToolResult result = handler.execute(new ToolExecutionContext(userId), new CurrentMonthSummaryToolInput())
                .data();

        assertEquals(new BigDecimal("3000.00"), result.totalIncome());
        assertEquals(LocalDate.of(2026, 5, 1), result.from());
        assertEquals(LocalDate.of(2026, 5, 18), result.to());
    }

    @Test
    void topSpendingCategories_sortsAndLimitsResults() {
        UUID userId = UUID.randomUUID();
        when(reportService.getCategorySpending(userId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 18)))
                .thenReturn(List.of(
                        CategorySpendingResponse.builder()
                                .categoryId(UUID.randomUUID())
                                .categoryName("Transport")
                                .totalAmount(new BigDecimal("200.00"))
                                .usagePercentage(new BigDecimal("11.11"))
                                .build(),
                        CategorySpendingResponse.builder()
                                .categoryId(UUID.randomUUID())
                                .categoryName("Food")
                                .totalAmount(new BigDecimal("600.00"))
                                .usagePercentage(new BigDecimal("33.33"))
                                .build()));

        TopSpendingCategoriesToolHandler handler = new TopSpendingCategoriesToolHandler(reportService, FIXED_CLOCK);

        TopSpendingCategoriesToolResult result = handler.execute(
                        new ToolExecutionContext(userId),
                        new TopSpendingCategoriesToolInput(1))
                .data();

        assertEquals(1, result.categories().size());
        assertEquals("Food", result.categories().getFirst().categoryName());
    }

    @Test
    void topSpendingCategories_rejectsNonPositiveLimit() {
        TopSpendingCategoriesToolHandler handler = new TopSpendingCategoriesToolHandler(reportService, FIXED_CLOCK);

        assertThrows(
                ToolValidationException.class,
                () -> handler.execute(new ToolExecutionContext(UUID.randomUUID()), new TopSpendingCategoriesToolInput(0)));
    }
}
