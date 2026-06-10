package com.saveapenny.report.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.saveapenny.report.dto.CashFlowPointResponse;
import com.saveapenny.report.dto.CategorySpendingResponse;
import com.saveapenny.report.dto.MonthlySummaryResponse;
import com.saveapenny.report.dto.NetWorthSnapshotResponse;
import com.saveapenny.report.repository.CashFlowPointView;
import com.saveapenny.report.repository.CategorySpendingView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ReportMapperTest {

    private final ReportMapper reportMapper = Mappers.getMapper(ReportMapper.class);

    @Test
    void toMonthlySummaryResponse_mapsFields() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);

        MonthlySummaryResponse response = reportMapper.toMonthlySummaryResponse(
                from, to, new BigDecimal("5000.0000"), new BigDecimal("3000.0000"), new BigDecimal("2000.0000"));

        assertNotNull(response);
        assertEquals(from, response.getStartDate());
        assertEquals(to, response.getEndDate());
        assertEquals(0, new BigDecimal("5000.0000").compareTo(response.getTotalIncome()));
        assertEquals(0, new BigDecimal("3000.0000").compareTo(response.getTotalExpense()));
        assertEquals(0, new BigDecimal("2000.0000").compareTo(response.getNetSavings()));
    }

    @Test
    void toCategorySpendingResponse_mapsFromView() {
        UUID categoryId = UUID.randomUUID();
        CategorySpendingView view = new CategorySpendingView() {
            @Override
            public UUID getCategoryId() {
                return categoryId;
            }

            @Override
            public String getCategoryName() {
                return "Food";
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal("450.0000");
            }
        };

        CategorySpendingResponse response = reportMapper.toCategorySpendingResponse(view);

        assertEquals(categoryId, response.getCategoryId());
        assertEquals("Food", response.getCategoryName());
        assertEquals(0, new BigDecimal("450.0000").compareTo(response.getTotalAmount()));
    }

    @Test
    void toCashFlowPointResponse_mapsFromView() {
        LocalDate date = LocalDate.of(2026, 5, 15);
        CashFlowPointView view = new CashFlowPointView() {
            @Override
            public LocalDate getDate() {
                return date;
            }

            @Override
            public BigDecimal getIncomeAmount() {
                return new BigDecimal("1000.0000");
            }

            @Override
            public BigDecimal getExpenseAmount() {
                return new BigDecimal("300.0000");
            }

            @Override
            public BigDecimal getNetAmount() {
                return new BigDecimal("700.0000");
            }
        };

        CashFlowPointResponse response = reportMapper.toCashFlowPointResponse(view);

        assertEquals(date, response.getDate());
        assertEquals(0, new BigDecimal("1000.0000").compareTo(response.getIncomeAmount()));
        assertEquals(0, new BigDecimal("300.0000").compareTo(response.getExpenseAmount()));
        assertEquals(0, new BigDecimal("700.0000").compareTo(response.getNetAmount()));
    }

    @Test
    void toNetWorthSnapshotResponse_mapsFields() {
        LocalDate snapshotDate = LocalDate.of(2026, 6, 1);

        NetWorthSnapshotResponse response = reportMapper.toNetWorthSnapshotResponse(
                snapshotDate, new BigDecimal("50000.0000"), new BigDecimal("5000.0000"), new BigDecimal("45000.0000"));

        assertEquals(snapshotDate, response.getSnapshotDate());
        assertEquals(0, new BigDecimal("50000.0000").compareTo(response.getTotalAssets()));
        assertEquals(0, new BigDecimal("5000.0000").compareTo(response.getTotalLiabilities()));
        assertEquals(0, new BigDecimal("45000.0000").compareTo(response.getNetWorth()));
    }
}
