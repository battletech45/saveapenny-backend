package com.saveapenny.insight.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.saveapenny.budget.dto.BudgetStatusResponse;
import com.saveapenny.budget.service.BudgetService;
import com.saveapenny.insight.entity.InsightType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CategoryInsightAnalyzerTest {

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private CategoryInsightAnalyzer analyzer;

    @Test
    void analyze_returnsCriticalWhenBudgetExceeded() {
        UUID userId = UUID.randomUUID();

        BudgetStatusResponse exceeded = BudgetStatusResponse.builder()
                .category("Groceries")
                .budgetAmount(new BigDecimal("500"))
                .spentAmount(new BigDecimal("600"))
                .remainingAmount(new BigDecimal("-100"))
                .usagePercentage(new BigDecimal("120"))
                .status("EXCEEDED")
                .build();

        when(budgetService.getStatuses(eq(userId), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(exceeded)));

        List<InsightCandidate> results = analyzer.analyze(userId);

        assertEquals(1, results.size());
        InsightCandidate candidate = results.getFirst();
        assertEquals(InsightType.RECOMMENDATION, candidate.type());
        assertEquals("CRITICAL", candidate.severity());
    }

    @Test
    void analyze_returnsWarningWhenBudgetNearingLimit() {
        UUID userId = UUID.randomUUID();

        BudgetStatusResponse warning = BudgetStatusResponse.builder()
                .category("Dining")
                .budgetAmount(new BigDecimal("300"))
                .spentAmount(new BigDecimal("270"))
                .remainingAmount(new BigDecimal("30"))
                .usagePercentage(new BigDecimal("90"))
                .status("WARNING")
                .build();

        when(budgetService.getStatuses(eq(userId), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(warning)));

        List<InsightCandidate> results = analyzer.analyze(userId);

        assertEquals(1, results.size());
        InsightCandidate candidate = results.getFirst();
        assertEquals(InsightType.RECOMMENDATION, candidate.type());
        assertEquals("WARNING", candidate.severity());
    }

    @Test
    void analyze_returnsEmptyWhenAllBudgetsOnTrack() {
        UUID userId = UUID.randomUUID();

        BudgetStatusResponse onTrack = BudgetStatusResponse.builder()
                .category("Utilities")
                .budgetAmount(new BigDecimal("200"))
                .spentAmount(new BigDecimal("100"))
                .remainingAmount(new BigDecimal("100"))
                .usagePercentage(new BigDecimal("50"))
                .status("ON_TRACK")
                .build();

        when(budgetService.getStatuses(eq(userId), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(onTrack)));

        List<InsightCandidate> results = analyzer.analyze(userId);

        assertEquals(0, results.size());
    }

    @Test
    void analyze_returnsEmptyWhenNoBudgetsExist() {
        UUID userId = UUID.randomUUID();

        when(budgetService.getStatuses(eq(userId), eq(null), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<InsightCandidate> results = analyzer.analyze(userId);

        assertEquals(0, results.size());
    }
}
