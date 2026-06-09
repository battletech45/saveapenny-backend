package com.saveapenny.insight.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.saveapenny.insight.config.InsightProperties;
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
class AnomalyDetectorTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private InsightProperties insightProperties;

    @InjectMocks
    private AnomalyDetector detector;

    @Test
    void analyze_returnsAnomalyWhenTransactionIsFarFromMean() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate now = LocalDate.now();
        LocalDate ninetyDaysAgo = now.minusDays(90);

        when(insightProperties.stddevThreshold()).thenReturn(1.0);

        TransactionResponse normal1 = tx(categoryId, "100.00", now.minusDays(10));
        TransactionResponse normal2 = tx(categoryId, "100.00", now.minusDays(8));
        TransactionResponse normal3 = tx(categoryId, "100.00", now.minusDays(6));
        TransactionResponse anomaly = tx(categoryId, "1000.00", now.minusDays(1));

        Page<TransactionResponse> page = new PageImpl<>(List.of(normal1, normal2, normal3, anomaly));
        when(transactionService.getAll(eq(userId), eq(ninetyDaysAgo), eq(now), eq(TransactionType.EXPENSE),
                eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(page);

        List<InsightCandidate> results = detector.analyze(userId);

        assertEquals(1, results.size());
        InsightCandidate candidate = results.getFirst();
        assertEquals(InsightType.ANOMALY, candidate.type());
        assertEquals(categoryId, candidate.categoryId());
        assertEquals("WARNING", candidate.severity());
    }

    @Test
    void analyze_returnsEmptyWhenAllTransactionsAreNormal() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate now = LocalDate.now();
        LocalDate ninetyDaysAgo = now.minusDays(90);

        when(insightProperties.stddevThreshold()).thenReturn(3.0);

        TransactionResponse tx1 = tx(categoryId, "100.00", now.minusDays(10));
        TransactionResponse tx2 = tx(categoryId, "110.00", now.minusDays(8));
        TransactionResponse tx3 = tx(categoryId, "90.00", now.minusDays(6));
        TransactionResponse tx4 = tx(categoryId, "105.00", now.minusDays(1));

        Page<TransactionResponse> page = new PageImpl<>(List.of(tx1, tx2, tx3, tx4));
        when(transactionService.getAll(eq(userId), eq(ninetyDaysAgo), eq(now), eq(TransactionType.EXPENSE),
                eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(page);

        List<InsightCandidate> results = detector.analyze(userId);

        assertEquals(0, results.size());
    }

    @Test
    void analyze_returnsEmptyWhenCategoryHasFewerThanThreeTransactions() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate now = LocalDate.now();
        LocalDate ninetyDaysAgo = now.minusDays(90);

        TransactionResponse tx1 = tx(categoryId, "100.00", now.minusDays(1));
        TransactionResponse tx2 = tx(categoryId, "200.00", now.minusDays(2));

        Page<TransactionResponse> page = new PageImpl<>(List.of(tx1, tx2));
        when(transactionService.getAll(eq(userId), eq(ninetyDaysAgo), eq(now), eq(TransactionType.EXPENSE),
                eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(page);

        List<InsightCandidate> results = detector.analyze(userId);

        assertEquals(0, results.size());
    }

    @Test
    void analyze_skipsTransactionsWithNullCategoryId() {
        UUID userId = UUID.randomUUID();

        LocalDate now = LocalDate.now();
        LocalDate ninetyDaysAgo = now.minusDays(90);

        TransactionResponse tx = TransactionResponse.builder()
                .id(UUID.randomUUID())
                .categoryId(null)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.EXPENSE)
                .transactionDate(now.minusDays(1))
                .build();

        Page<TransactionResponse> page = new PageImpl<>(List.of(tx));
        when(transactionService.getAll(eq(userId), eq(ninetyDaysAgo), eq(now), eq(TransactionType.EXPENSE),
                eq(null), eq(null), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(page);

        List<InsightCandidate> results = detector.analyze(userId);

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
}
