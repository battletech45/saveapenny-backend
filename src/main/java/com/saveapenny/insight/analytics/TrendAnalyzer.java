package com.saveapenny.insight.analytics;

import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class TrendAnalyzer {

    private final TransactionService transactionService;

    public TrendAnalyzer(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public List<InsightCandidate> analyze(UUID userId) {
        List<InsightCandidate> results = new ArrayList<>();

        LocalDate now = LocalDate.now();
        List<YearMonth> last3Months = new ArrayList<>();
        for (int i = 2; i >= 0; i--) {
            last3Months.add(YearMonth.from(now).minusMonths(i));
        }

        Map<UUID, List<BigDecimal>> monthlyByCategory = new HashMap<>();

        for (YearMonth ym : last3Months) {
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            Map<UUID, BigDecimal> categorySpending = getCategorySpending(userId, start, end);
            for (Map.Entry<UUID, BigDecimal> entry : categorySpending.entrySet()) {
                monthlyByCategory.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
            }
        }

        for (Map.Entry<UUID, List<BigDecimal>> entry : monthlyByCategory.entrySet()) {
            UUID categoryId = entry.getKey();
            List<BigDecimal> amounts = entry.getValue();

            if (amounts.size() < 3) {
                continue;
            }

            BigDecimal first = amounts.get(0);
            BigDecimal last = amounts.get(amounts.size() - 1);

            if (first.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            boolean consistentlyUp = true;
            boolean consistentlyDown = true;

            for (int i = 1; i < amounts.size(); i++) {
                int cmp = amounts.get(i).compareTo(amounts.get(i - 1));
                if (cmp < 0) consistentlyUp = false;
                if (cmp > 0) consistentlyDown = false;
            }

            if ((consistentlyUp || consistentlyDown) && last.compareTo(first) != 0) {
                String direction = consistentlyUp ? "increasing" : "decreasing";
                BigDecimal totalChange = last.subtract(first).divide(first, 4, RoundingMode.HALF_UP);

                results.add(new InsightCandidate(
                        com.saveapenny.insight.entity.InsightType.TREND,
                        "Spending trend detected",
                        String.format("Your spending has been %s over the last 3 months in this category.",
                                direction),
                        String.format("Month 1: $%s, Month 2: $%s, Month 3: $%s (total change: %s%%)",
                                amounts.get(0).setScale(2, RoundingMode.HALF_UP),
                                amounts.get(1).setScale(2, RoundingMode.HALF_UP),
                                amounts.get(2).setScale(2, RoundingMode.HALF_UP),
                                totalChange.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)),
                        categoryId,
                        consistentlyUp ? "WARNING" : "INFO",
                        null));
            }
        }

        return results;
    }

    private Map<UUID, BigDecimal> getCategorySpending(UUID userId, LocalDate from, LocalDate to) {
        Map<UUID, BigDecimal> spending = new HashMap<>();
        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            var txPage = transactionService.getAll(userId, from, to, TransactionType.EXPENSE, null, null, null, null, null, PageRequest.of(page, 100));
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
