package com.saveapenny.insight.analytics;

import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class SpendingPatternAnalyzer {

    private static final BigDecimal THRESHOLD = new BigDecimal("0.20");

    private final TransactionService transactionService;

    public SpendingPatternAnalyzer(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public List<InsightCandidate> analyze(UUID userId) {
        List<InsightCandidate> results = new ArrayList<>();

        LocalDate now = LocalDate.now();
        LocalDate currentStart = now.withDayOfMonth(1);
        LocalDate previousStart = currentStart.minusMonths(1);
        LocalDate previousEnd = currentStart.minusDays(1);

        Map<UUID, BigDecimal> currentByCategory = getCategorySpending(userId, TransactionType.EXPENSE, currentStart, now);
        Map<UUID, BigDecimal> previousByCategory = getCategorySpending(userId, TransactionType.EXPENSE, previousStart, previousEnd);

        for (Map.Entry<UUID, BigDecimal> entry : currentByCategory.entrySet()) {
            UUID categoryId = entry.getKey();
            BigDecimal currentAmount = entry.getValue();
            BigDecimal previousAmount = previousByCategory.getOrDefault(categoryId, BigDecimal.ZERO);

            if (previousAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal change = currentAmount.subtract(previousAmount)
                    .divide(previousAmount, 4, RoundingMode.HALF_UP);

            if (change.abs().compareTo(THRESHOLD) >= 0) {
                String direction = change.compareTo(BigDecimal.ZERO) > 0 ? "increased" : "decreased";
                String severity = change.abs().compareTo(new BigDecimal("0.50")) >= 0 ? "WARNING" : "INFO";

                results.add(new InsightCandidate(
                        com.saveapenny.insight.entity.InsightType.SPENDING_PATTERN,
                        "Spending pattern detected for category",
                        String.format("Your spending %s by %s%% compared to last month.",
                                direction, change.abs().multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)),
                        String.format("Current period: $%s, Previous period: $%s",
                                currentAmount.setScale(2, RoundingMode.HALF_UP),
                                previousAmount.setScale(2, RoundingMode.HALF_UP)),
                        categoryId,
                        severity,
                        null));
            }
        }

        return results;
    }

    private Map<UUID, BigDecimal> getCategorySpending(UUID userId, TransactionType type, LocalDate from, LocalDate to) {
        Map<UUID, BigDecimal> spending = new HashMap<>();
        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            var txPage = transactionService.getAll(userId, from, to, type, null, null, null, null, null, PageRequest.of(page, 100));
            for (TransactionResponse tx : txPage.getContent()) {
                if (tx.getCategoryId() != null) {
                    spending.merge(tx.getCategoryId(), tx.getAmount(), BigDecimal::add);
                }
            }
            hasMore = txPage.hasNext();
            page++;
        }

        return spending;
    }
}
