package com.saveapenny.report.mapper;

import com.saveapenny.report.dto.CashFlowPointResponse;
import com.saveapenny.report.dto.CategorySpendingResponse;
import com.saveapenny.report.dto.MonthlySummaryResponse;
import com.saveapenny.report.dto.NetWorthSnapshotResponse;
import com.saveapenny.report.repository.CashFlowPointView;
import com.saveapenny.report.repository.CategorySpendingView;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReportMapper {

    @Mapping(target = "usagePercentage", ignore = true)
    CategorySpendingResponse toCategorySpendingResponse(CategorySpendingView view);

    CashFlowPointResponse toCashFlowPointResponse(CashFlowPointView view);

    default MonthlySummaryResponse toMonthlySummaryResponse(
            LocalDate from,
            LocalDate to,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal netSavings) {
        return MonthlySummaryResponse.builder()
                .startDate(from)
                .endDate(to)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netSavings(netSavings)
                .build();
    }

    default NetWorthSnapshotResponse toNetWorthSnapshotResponse(
            LocalDate snapshotDate,
            BigDecimal totalAssets,
            BigDecimal totalLiabilities,
            BigDecimal netWorth) {
        return NetWorthSnapshotResponse.builder()
                .snapshotDate(snapshotDate)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .netWorth(netWorth)
                .build();
    }
}
