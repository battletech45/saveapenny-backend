package com.saveapenny.insight.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.saveapenny.insight.entity.InsightType;
import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class SpendingPatternAnalyzerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private SpendingPatternAnalyzer analyzer;

    @Test
    void analyze_returnsInsightWhenSpendingIncreased() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        LocalDate now = LocalDate.now();
        LocalDate currentStart = now.withDayOfMonth(1);
        LocalDate previousStart = currentStart.minusMonths(1);
        LocalDate previousEnd = currentStart.minusDays(1);

        TransactionResponse currentTx = TransactionResponse.builder()
                .id(UUID.randomUUID())
                .categoryId(categoryId)
                .amount(new BigDecimal("300.00"))
                .type(TransactionType.EXPENSE)
                .transactionDate(now)
                .build();

        TransactionResponse previousTx = TransactionResponse.builder()
                .id(UUID.randomUUID())
                .categoryId(categoryId)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.EXPENSE)
                .transactionDate(previousStart)
                .build();

        Page<TransactionResponse> currentPage = new PageImpl<>(List.of(currentTx));
        Page<TransactionResponse> previousPage = new PageImpl<>(List.of(previousTx));

        when(transactionService.getAll(eq(userId), eq(currentStart), eq(now), eq(TransactionType.EXPENSE),
                eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(currentPage);

        when(transactionService.getAll(eq(userId), eq(previousStart), eq(previousEnd), eq(TransactionType.EXPENSE),
                eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(previousPage);

        List<InsightCandidate> results = analyzer.analyze(userId);

        assertEquals(1, results.size());
        InsightCandidate candidate = results.getFirst();
        assertEquals(InsightType.SPENDING_PATTERN, candidate.type());
        assertEquals(categoryId, candidate.categoryId());
    }

    @Test
    void analyze_returnsEmptyWhenNoSignificantChange() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        LocalDate now = LocalDate.now();
        LocalDate currentStart = now.withDayOfMonth(1);
        LocalDate previousStart = currentStart.minusMonths(1);
        LocalDate previousEnd = currentStart.minusDays(1);

        TransactionResponse tx = TransactionResponse.builder()
                .id(UUID.randomUUID())
                .categoryId(categoryId)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.EXPENSE)
                .transactionDate(now)
                .build();

        Page<TransactionResponse> page = new PageImpl<>(List.of(tx, tx));

        when(transactionService.getAll(eq(userId), eq(currentStart), eq(now), eq(TransactionType.EXPENSE),
                eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(page);

        when(transactionService.getAll(eq(userId), eq(previousStart), eq(previousEnd), eq(TransactionType.EXPENSE),
                eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(page);

        List<InsightCandidate> results = analyzer.analyze(userId);

        assertEquals(0, results.size());
    }

    @Test
    void analyze_returnsEmptyWhenNoPreviousSpending() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        LocalDate now = LocalDate.now();
        LocalDate currentStart = now.withDayOfMonth(1);
        LocalDate previousStart = currentStart.minusMonths(1);
        LocalDate previousEnd = currentStart.minusDays(1);

        TransactionResponse tx = TransactionResponse.builder()
                .id(UUID.randomUUID())
                .categoryId(categoryId)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.EXPENSE)
                .transactionDate(now)
                .build();

        Page<TransactionResponse> currentPage = new PageImpl<>(List.of(tx));
        Page<TransactionResponse> emptyPage = new PageImpl<>(List.of());

        when(transactionService.getAll(eq(userId), eq(currentStart), eq(now), eq(TransactionType.EXPENSE),
                eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(currentPage);

        when(transactionService.getAll(eq(userId), eq(previousStart), eq(previousEnd), eq(TransactionType.EXPENSE),
                eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(emptyPage);

        List<InsightCandidate> results = analyzer.analyze(userId);

        assertEquals(0, results.size());
    }
}
