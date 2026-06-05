package com.saveapenny.mcp.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TopSpendingCategoriesToolResult(
        LocalDate from,
        LocalDate to,
        List<TopSpendingCategoryItem> categories) {

    public record TopSpendingCategoryItem(
            UUID categoryId,
            String categoryName,
            BigDecimal totalAmount,
            BigDecimal usagePercentage) {
    }
}
