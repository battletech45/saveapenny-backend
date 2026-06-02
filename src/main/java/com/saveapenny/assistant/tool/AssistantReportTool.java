package com.saveapenny.assistant.tool;

import com.saveapenny.report.dto.CategorySpendingResponse;
import com.saveapenny.report.dto.MonthlySummaryResponse;
import com.saveapenny.report.service.ReportService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class AssistantReportTool {

    private final ReportService reportService;
    private final AssistantToolContextHolder assistantToolContextHolder;

    public AssistantReportTool(ReportService reportService, AssistantToolContextHolder assistantToolContextHolder) {
        this.reportService = reportService;
        this.assistantToolContextHolder = assistantToolContextHolder;
    }

    @Tool(name = "getCurrentMonthSummary", description = "Get the authenticated user's current month income, expense, and net savings summary.")
    public String getCurrentMonthSummary() {
        UUID userId = assistantToolContextHolder.requireCurrentUserId();
        LocalDate from = LocalDate.now().withDayOfMonth(1);
        LocalDate to = LocalDate.now();

        MonthlySummaryResponse summary = reportService.getMonthlySummary(userId, from, to);
        return "Current month summary: income="
                + summary.getTotalIncome()
                + ", expense="
                + summary.getTotalExpense()
                + ", netSavings="
                + summary.getNetSavings()
                + '.';
    }

    @Tool(name = "getTopSpendingCategories", description = "Get the authenticated user's top expense categories for the current month.")
    public String getTopSpendingCategories(
            @ToolParam(description = "Maximum number of categories to return.", required = false) int topCategoriesLimit) {
        UUID userId = assistantToolContextHolder.requireCurrentUserId();
        LocalDate from = LocalDate.now().withDayOfMonth(1);
        LocalDate to = LocalDate.now();

        List<CategorySpendingResponse> topCategories = reportService.getCategorySpending(userId, from, to)
                .stream()
                .sorted(Comparator.comparing(CategorySpendingResponse::getTotalAmount).reversed())
                .limit(Math.max(0, topCategoriesLimit))
                .toList();

        if (topCategories.isEmpty()) {
            return "Top spending categories: none for the current month.";
        }

        StringBuilder builder = new StringBuilder("Top spending categories: ");
        for (int i = 0; i < topCategories.size(); i++) {
            CategorySpendingResponse item = topCategories.get(i);
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(item.getCategoryName())
                    .append('=')
                    .append(item.getTotalAmount())
                    .append(" (")
                    .append(item.getUsagePercentage())
                    .append("%)");
        }
        builder.append('.');
        return builder.toString();
    }
}
