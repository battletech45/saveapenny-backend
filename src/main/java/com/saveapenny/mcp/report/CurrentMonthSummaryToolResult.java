package com.saveapenny.mcp.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CurrentMonthSummaryToolResult(
        LocalDate from,
        LocalDate to,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netSavings) {
}
