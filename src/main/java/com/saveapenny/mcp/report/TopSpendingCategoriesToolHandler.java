package com.saveapenny.mcp.report;

import com.saveapenny.mcp.definition.ToolDataType;
import com.saveapenny.mcp.definition.ToolDefinition;
import com.saveapenny.mcp.definition.ToolPropertyDefinition;
import com.saveapenny.mcp.definition.ToolSchemaDefinition;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import com.saveapenny.mcp.execution.ToolHandler;
import com.saveapenny.mcp.execution.ToolResult;
import com.saveapenny.mcp.execution.ToolValidationSupport;
import com.saveapenny.report.dto.CategorySpendingResponse;
import com.saveapenny.report.service.ReportService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TopSpendingCategoriesToolHandler implements ToolHandler<TopSpendingCategoriesToolInput, TopSpendingCategoriesToolResult> {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "getTopSpendingCategories",
            "Get the authenticated user's top expense categories for the current month.",
            new ToolSchemaDefinition(
                    "TopSpendingCategoriesToolInput",
                    ToolDataType.OBJECT,
                    "Input for top spending category lookup.",
                    List.of(new ToolPropertyDefinition("limit", ToolDataType.INTEGER, "Maximum number of categories to return.", false))),
            new ToolSchemaDefinition(
                    "TopSpendingCategoriesToolResult",
                    ToolDataType.OBJECT,
                    "Top expense categories for the current month.",
                    List.of(
                            new ToolPropertyDefinition("from", ToolDataType.STRING, "Report start date.", true),
                            new ToolPropertyDefinition("to", ToolDataType.STRING, "Report end date.", true),
                            new ToolPropertyDefinition("categories", ToolDataType.ARRAY, "Ranked category results.", true))),
            TopSpendingCategoriesToolInput.class,
            TopSpendingCategoriesToolResult.class);

    private final ReportService reportService;
    private final Clock clock;

    public TopSpendingCategoriesToolHandler(ReportService reportService, Clock clock) {
        this.reportService = reportService;
        this.clock = clock;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public void validate(ToolExecutionContext context, TopSpendingCategoriesToolInput input) {
        if (input != null) {
            ToolValidationSupport.requirePositiveInteger(input.limit(), "limit", "limit");
        }
    }

    @Override
    public ToolResult<TopSpendingCategoriesToolResult> doExecute(
            ToolExecutionContext context,
            TopSpendingCategoriesToolInput input) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.withDayOfMonth(1);
        List<TopSpendingCategoriesToolResult.TopSpendingCategoryItem> categories = reportService
                .getCategorySpending(context.requireUserId(), from, today)
                .stream()
                .sorted(Comparator.comparing(CategorySpendingResponse::getTotalAmount).reversed())
                .limit(input == null ? 5 : input.normalizedLimit())
                .map(item -> new TopSpendingCategoriesToolResult.TopSpendingCategoryItem(
                        item.getCategoryId(),
                        item.getCategoryName(),
                        item.getTotalAmount(),
                        item.getUsagePercentage()))
                .toList();

        return ToolResult.of(new TopSpendingCategoriesToolResult(from, today, categories));
    }
}
