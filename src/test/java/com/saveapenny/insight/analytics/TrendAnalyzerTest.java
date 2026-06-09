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
import java.time.YearMonth;
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
class TrendAnalyzerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TrendAnalyzer analyzer;

    @Test
    void analyze_returnsIncreasingTrendWhenSpendingRises() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        YearMonth thisMonth = YearMonth.from(LocalDate.now());
        YearMonth monthMinus1 = thisMonth.minusMonths(1);
        YearMonth monthMinus2 = thisMonth.minusMonths(2);

        Page<TransactionResponse> month1page = pageOf(tx(categoryId, "50.00", monthMinus2.atEndOfMonth()));
        Page<TransactionResponse> month2page = pageOf(tx(categoryId, "75.00", monthMinus1.atEndOfMonth()));
        Page<TransactionResponse> month3page = pageOf(tx(categoryId, "100.00", thisMonth.atDay(1)));

        when(transactionService.getAll(eq(userId), eq(monthMinus2.atDay(1)), eq(monthMinus2.atEndOfMonth()),
                eq(TransactionType.EXPENSE), eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(month1page);
        when(transactionService.getAll(eq(userId), eq(monthMinus1.atDay(1)), eq(monthMinus1.atEndOfMonth()),
                eq(TransactionType.EXPENSE), eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(month2page);
        when(transactionService.getAll(eq(userId), eq(thisMonth.atDay(1)), eq(thisMonth.atEndOfMonth()),
                eq(TransactionType.EXPENSE), eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(month3page);

        List<InsightCandidate> results = analyzer.analyze(userId);

        assertEquals(1, results.size());
        InsightCandidate candidate = results.getFirst();
        assertEquals(InsightType.TREND, candidate.type());
        assertEquals(categoryId, candidate.categoryId());
        assertEquals("WARNING", candidate.severity());
    }

    @Test
    void analyze_returnsDecreasingTrendWhenSpendingDrops() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        YearMonth thisMonth = YearMonth.from(LocalDate.now());
        YearMonth monthMinus1 = thisMonth.minusMonths(1);
        YearMonth monthMinus2 = thisMonth.minusMonths(2);

        Page<TransactionResponse> month1page = pageOf(tx(categoryId, "100.00", monthMinus2.atEndOfMonth()));
        Page<TransactionResponse> month2page = pageOf(tx(categoryId, "75.00", monthMinus1.atEndOfMonth()));
        Page<TransactionResponse> month3page = pageOf(tx(categoryId, "50.00", thisMonth.atDay(1)));

        when(transactionService.getAll(eq(userId), eq(monthMinus2.atDay(1)), eq(monthMinus2.atEndOfMonth()),
                eq(TransactionType.EXPENSE), eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(month1page);
        when(transactionService.getAll(eq(userId), eq(monthMinus1.atDay(1)), eq(monthMinus1.atEndOfMonth()),
                eq(TransactionType.EXPENSE), eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(month2page);
        when(transactionService.getAll(eq(userId), eq(thisMonth.atDay(1)), eq(thisMonth.atEndOfMonth()),
                eq(TransactionType.EXPENSE), eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(month3page);

        List<InsightCandidate> results = analyzer.analyze(userId);

        assertEquals(1, results.size());
        InsightCandidate candidate = results.getFirst();
        assertEquals(InsightType.TREND, candidate.type());
        assertEquals(categoryId, candidate.categoryId());
        assertEquals("INFO", candidate.severity());
    }

    @Test
    void analyze_returnsEmptyWhenSpendingIsFlat() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        YearMonth thisMonth = YearMonth.from(LocalDate.now());
        YearMonth monthMinus1 = thisMonth.minusMonths(1);
        YearMonth monthMinus2 = thisMonth.minusMonths(2);

        Page<TransactionResponse> month1page = pageOf(tx(categoryId, "80.00", monthMinus2.atEndOfMonth()));
        Page<TransactionResponse> month2page = pageOf(tx(categoryId, "80.00", monthMinus1.atEndOfMonth()));
        Page<TransactionResponse> month3page = pageOf(tx(categoryId, "80.00", thisMonth.atDay(1)));

        when(transactionService.getAll(eq(userId), eq(monthMinus2.atDay(1)), eq(monthMinus2.atEndOfMonth()),
                eq(TransactionType.EXPENSE), eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(month1page);
        when(transactionService.getAll(eq(userId), eq(monthMinus1.atDay(1)), eq(monthMinus1.atEndOfMonth()),
                eq(TransactionType.EXPENSE), eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(month2page);
        when(transactionService.getAll(eq(userId), eq(thisMonth.atDay(1)), eq(thisMonth.atEndOfMonth()),
                eq(TransactionType.EXPENSE), eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(month3page);

        List<InsightCandidate> results = analyzer.analyze(userId);

        assertEquals(0, results.size());
    }

    @Test
    void analyze_returnsEmptyWhenNoTransactionsExist() {
        UUID userId = UUID.randomUUID();

        Page<TransactionResponse> emptyPage = new PageImpl<>(List.of());

        when(transactionService.getAll(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(emptyPage);

        List<InsightCandidate> results = analyzer.analyze(userId);

        assertEquals(0, results.size());
    }

    private static TransactionResponse tx(UUID categoryId, String amount, LocalDate date) {
        return TransactionResponse.builder()
                .id(UUID.randomUUID())
                .categoryId(categoryId)
                .amount(new BigDecimal(amount))
                .type(TransactionType.EXPENSE)
                .transactionDate(date)
                .build();
    }

    private static Page<TransactionResponse> pageOf(TransactionResponse... txs) {
        return new PageImpl<>(List.of(txs));
    }
}
