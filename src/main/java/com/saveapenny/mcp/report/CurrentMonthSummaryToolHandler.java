package com.saveapenny.mcp.report;

import com.saveapenny.mcp.definition.ToolDataType;
import com.saveapenny.mcp.definition.ToolDefinition;
import com.saveapenny.mcp.definition.ToolPropertyDefinition;
import com.saveapenny.mcp.definition.ToolSchemaDefinition;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import com.saveapenny.mcp.execution.ToolHandler;
import com.saveapenny.mcp.execution.ToolResult;
import com.saveapenny.report.dto.MonthlySummaryResponse;
import com.saveapenny.report.service.ReportService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CurrentMonthSummaryToolHandler implements ToolHandler<CurrentMonthSummaryToolInput, CurrentMonthSummaryToolResult> {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "getCurrentMonthSummary",
            "Get the authenticated user's current month income, expense, and net savings summary.",
            new ToolSchemaDefinition(
                    "CurrentMonthSummaryToolInput",
                    ToolDataType.OBJECT,
                    "No input fields are required for this tool.",
                    List.of()),
            new ToolSchemaDefinition(
                    "CurrentMonthSummaryToolResult",
                    ToolDataType.OBJECT,
                    "Current month financial summary.",
                    List.of(
                            new ToolPropertyDefinition("from", ToolDataType.STRING, "Summary start date.", true),
                            new ToolPropertyDefinition("to", ToolDataType.STRING, "Summary end date.", true),
                            new ToolPropertyDefinition("totalIncome", ToolDataType.NUMBER, "Total income in range.", true),
                            new ToolPropertyDefinition("totalExpense", ToolDataType.NUMBER, "Total expense in range.", true),
                            new ToolPropertyDefinition("netSavings", ToolDataType.NUMBER, "Net savings in range.", true))),
            CurrentMonthSummaryToolInput.class,
            CurrentMonthSummaryToolResult.class);

    private final ReportService reportService;
    private final Clock clock;

    public CurrentMonthSummaryToolHandler(ReportService reportService, Clock clock) {
        this.reportService = reportService;
        this.clock = clock;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolResult<CurrentMonthSummaryToolResult> doExecute(
            ToolExecutionContext context,
            CurrentMonthSummaryToolInput input) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.withDayOfMonth(1);
        MonthlySummaryResponse summary = reportService.getMonthlySummary(context.requireUserId(), from, today);
        return ToolResult.of(new CurrentMonthSummaryToolResult(
                summary.getStartDate(),
                summary.getEndDate(),
                summary.getTotalIncome(),
                summary.getTotalExpense(),
                summary.getNetSavings()));
    }
}
