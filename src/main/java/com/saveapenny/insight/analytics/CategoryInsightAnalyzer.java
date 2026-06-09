package com.saveapenny.insight.analytics;

import com.saveapenny.budget.dto.BudgetStatusResponse;
import com.saveapenny.budget.service.BudgetService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class CategoryInsightAnalyzer {

    private final BudgetService budgetService;

    public CategoryInsightAnalyzer(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    public List<InsightCandidate> analyze(UUID userId) {
        List<InsightCandidate> results = new ArrayList<>();

        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            var statusPage = budgetService.getStatuses(userId, null, PageRequest.of(page, 50));
            for (BudgetStatusResponse status : statusPage.getContent()) {
                String budgetStatus = status.getStatus();

                if ("EXCEEDED".equals(budgetStatus)) {
                    results.add(new InsightCandidate(
                            com.saveapenny.insight.entity.InsightType.RECOMMENDATION,
                            "Budget exceeded",
                            String.format("You have exceeded your %s budget.", status.getCategory()),
                            String.format("Budget: $%s, Spent: $%s, Usage: %s%%",
                                    status.getBudgetAmount(),
                                    status.getSpentAmount(),
                                    status.getUsagePercentage()),
                            null,
                            "CRITICAL",
                            null));
                } else if ("WARNING".equals(budgetStatus)) {
                    results.add(new InsightCandidate(
                            com.saveapenny.insight.entity.InsightType.RECOMMENDATION,
                            "Budget nearing limit",
                            String.format("You have used %s%% of your %s budget.", status.getUsagePercentage(), status.getCategory()),
                            String.format("Budget: $%s, Spent: $%s, Remaining: $%s",
                                    status.getBudgetAmount(),
                                    status.getSpentAmount(),
                                    status.getRemainingAmount()),
                            null,
                            "WARNING",
                            null));
                }
            }
            hasMore = statusPage.hasNext();
            page++;
        }

        return results;
    }
}
