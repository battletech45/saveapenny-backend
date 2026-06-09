package com.saveapenny.insight.analytics;

import com.saveapenny.insight.config.InsightProperties;
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
public class AnomalyDetector {

    private final TransactionService transactionService;
    private final InsightProperties insightProperties;

    public AnomalyDetector(TransactionService transactionService, InsightProperties insightProperties) {
        this.transactionService = transactionService;
        this.insightProperties = insightProperties;
    }

    public List<InsightCandidate> analyze(UUID userId) {
        List<InsightCandidate> results = new ArrayList<>();

        LocalDate now = LocalDate.now();
        LocalDate ninetyDaysAgo = now.minusDays(90);

        List<TransactionResponse> transactions = fetchExpenses(userId, ninetyDaysAgo, now);

        Map<UUID, List<BigDecimal>> amountsByCategory = new HashMap<>();
        for (TransactionResponse tx : transactions) {
            if (tx.getCategoryId() != null) {
                amountsByCategory.computeIfAbsent(tx.getCategoryId(), k -> new ArrayList<>()).add(tx.getAmount());
            }
        }

        for (Map.Entry<UUID, List<BigDecimal>> entry : amountsByCategory.entrySet()) {
            UUID categoryId = entry.getKey();
            List<BigDecimal> amounts = entry.getValue();

            if (amounts.size() < 3) {
                continue;
            }

            BigDecimal mean = mean(amounts);
            BigDecimal stddev = stddev(amounts, mean);

            if (stddev.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal threshold = stddev.multiply(BigDecimal.valueOf(insightProperties.stddevThreshold()));

            for (TransactionResponse tx : transactions) {
                if (!categoryId.equals(tx.getCategoryId())) {
                    continue;
                }

                BigDecimal diff = tx.getAmount().subtract(mean).abs();
                if (diff.compareTo(threshold) > 0) {
                    results.add(new InsightCandidate(
                            com.saveapenny.insight.entity.InsightType.ANOMALY,
                            "Unusual transaction detected",
                            String.format("A transaction of $%s is unusually high compared to your typical spending in this category.",
                                    tx.getAmount().setScale(2, RoundingMode.HALF_UP)),
                            String.format("Amount: $%s, Category average: $%s, StdDev: $%s",
                                    tx.getAmount().setScale(2, RoundingMode.HALF_UP),
                                    mean.setScale(2, RoundingMode.HALF_UP),
                                    stddev.setScale(2, RoundingMode.HALF_UP)),
                            categoryId,
                            "WARNING",
                            null));
                }
            }
        }

        return results;
    }

    private List<TransactionResponse> fetchExpenses(UUID userId, LocalDate from, LocalDate to) {
        List<TransactionResponse> all = new ArrayList<>();
        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            var txPage = transactionService.getAll(userId, from, to, TransactionType.EXPENSE, null, null, null, null, null, PageRequest.of(page, 200));
            all.addAll(txPage.getContent());
            hasMore = txPage.hasNext();
            page++;
        }

        return all;
    }

    private BigDecimal mean(List<BigDecimal> values) {
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal stddev(List<BigDecimal> values, BigDecimal mean) {
        BigDecimal variance = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }
}
