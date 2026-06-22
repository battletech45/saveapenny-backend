package com.saveapenny.report.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.saveapenny.account.entity.AccountType;
import com.saveapenny.config.TimeService;
import com.saveapenny.report.dto.CashFlowPointResponse;
import java.util.List;
import com.saveapenny.report.dto.CategorySpendingResponse;
import com.saveapenny.report.dto.MonthlySummaryResponse;
import com.saveapenny.report.dto.NetWorthSnapshotResponse;
import com.saveapenny.report.exception.InvalidNetWorthSnapshotDateException;
import com.saveapenny.report.exception.InvalidReportDateRangeException;
import com.saveapenny.report.entity.NetWorthSnapshot;
import com.saveapenny.report.mapper.ReportMapper;
import com.saveapenny.report.repository.CashFlowPointView;
import com.saveapenny.report.repository.CategorySpendingView;
import com.saveapenny.report.repository.NetWorthSnapshotRepository;
import com.saveapenny.report.repository.ReportAccountRepository;
import com.saveapenny.report.repository.ReportTransactionRepository;
import com.saveapenny.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 19);

    @Mock
    private ReportTransactionRepository reportTransactionRepository;
    @Mock
    private ReportAccountRepository reportAccountRepository;
    @Mock
    private NetWorthSnapshotRepository netWorthSnapshotRepository;
    @Mock
    private ReportMapper reportMapper;
    @Mock
    private TimeService timeService;

    @InjectMocks
    private ReportServiceImpl reportService;

    private UUID userId;
    private LocalDate from;
    private LocalDate to;

    @BeforeEach
    void setUp() {
        lenient().when(timeService.today()).thenReturn(TODAY);
        userId = UUID.randomUUID();
        from = LocalDate.of(2026, 5, 1);
        to = LocalDate.of(2026, 5, 31);
    }

    @Test
    void getMonthlySummary_returnsMappedSummary_whenValidRange() {
        when(reportTransactionRepository.sumAmountByUserIdAndTypeAndTransactionDateBetween(
                        userId, TransactionType.INCOME, from, to))
                .thenReturn(new BigDecimal("1500.0000"));
        when(reportTransactionRepository.sumAmountByUserIdAndTypeAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, from, to))
                .thenReturn(new BigDecimal("500.0000"));

        MonthlySummaryResponse mapped = MonthlySummaryResponse.builder()
                .startDate(from)
                .endDate(to)
                .totalIncome(new BigDecimal("1500.0000"))
                .totalExpense(new BigDecimal("500.0000"))
                .netSavings(new BigDecimal("1000.0000"))
                .build();
        when(reportMapper.toMonthlySummaryResponse(
                        from,
                        to,
                        new BigDecimal("1500.0000"),
                        new BigDecimal("500.0000"),
                        new BigDecimal("1000.0000")))
                .thenReturn(mapped);

        MonthlySummaryResponse result = reportService.getMonthlySummary(userId, from, to);

        assertEquals(new BigDecimal("1000.0000"), result.getNetSavings());
    }

    @Test
    void getCategorySpending_setsUsagePercentage_whenTotalExpenseExists() {
        CategorySpendingView raw = new CategorySpendingView() {
            @Override
            public UUID getCategoryId() {
                return UUID.randomUUID();
            }

            @Override
            public String getCategoryName() {
                return "Food";
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal("300.0000");
            }
        };

        when(reportTransactionRepository.sumAmountByUserIdAndTypeAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, from, to))
                .thenReturn(new BigDecimal("1200.0000"));
        when(reportTransactionRepository.findCategorySpendingByUserIdAndTransactionDateBetween(userId, from, to))
                .thenReturn(List.of(raw));
        when(reportMapper.toCategorySpendingResponse(raw)).thenReturn(CategorySpendingResponse.builder()
                .categoryId(raw.getCategoryId())
                .categoryName("Food")
                .totalAmount(new BigDecimal("300.0000"))
                .build());

        List<CategorySpendingResponse> result = reportService.getCategorySpending(userId, from, to);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("25.00"), result.get(0).getUsagePercentage());
    }

    @Test
    void getCashFlow_returnsMappedRows() {
        CashFlowPointView row = new CashFlowPointView() {
            @Override
            public LocalDate getDate() {
                return LocalDate.of(2026, 5, 3);
            }

            @Override
            public BigDecimal getIncomeAmount() {
                return new BigDecimal("200.0000");
            }

            @Override
            public BigDecimal getExpenseAmount() {
                return new BigDecimal("50.0000");
            }

            @Override
            public BigDecimal getNetAmount() {
                return new BigDecimal("150.0000");
            }
        };

        CashFlowPointResponse mapped = CashFlowPointResponse.builder()
                .date(row.getDate())
                .incomeAmount(row.getIncomeAmount())
                .expenseAmount(row.getExpenseAmount())
                .netAmount(row.getNetAmount())
                .build();

        when(reportTransactionRepository.findCashFlowByUserIdAndTransactionDateBetween(userId, from, to))
                .thenReturn(List.of(row));
        when(reportMapper.toCashFlowPointResponse(row)).thenReturn(mapped);

        List<CashFlowPointResponse> result = reportService.getCashFlow(userId, from, to);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("150.0000"), result.get(0).getNetAmount());
    }

    @Test
    void getNetWorth_returnsExistingSnapshot_whenFound() {
        LocalDate snapshotDate = TODAY.minusDays(1);
        NetWorthSnapshot snapshot = NetWorthSnapshot.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .snapshotDate(snapshotDate)
                .totalAssets(new BigDecimal("5000.0000"))
                .totalLiabilities(new BigDecimal("1200.0000"))
                .netWorth(new BigDecimal("3800.0000"))
                .build();

        when(netWorthSnapshotRepository.findByUserIdAndSnapshotDate(userId, snapshotDate))
                .thenReturn(Optional.of(snapshot));

        NetWorthSnapshotResponse mapped = NetWorthSnapshotResponse.builder()
                .snapshotDate(snapshotDate)
                .totalAssets(new BigDecimal("5000.0000"))
                .totalLiabilities(new BigDecimal("1200.0000"))
                .netWorth(new BigDecimal("3800.0000"))
                .build();
        when(reportMapper.toNetWorthSnapshotResponse(snapshot))
                .thenReturn(mapped);

        NetWorthSnapshotResponse result = reportService.getNetWorth(userId, snapshotDate);

        assertEquals(new BigDecimal("3800.0000"), result.getNetWorth());
    }

    @Test
    void getNetWorth_computesAndStoresSnapshot_whenNotFound() {
        LocalDate snapshotDate = TODAY.minusDays(1);

        when(netWorthSnapshotRepository.findByUserIdAndSnapshotDate(userId, snapshotDate))
                .thenReturn(Optional.empty());
        when(reportAccountRepository.sumAssetsByUserId(userId, List.of(AccountType.CREDIT))).thenReturn(new BigDecimal("5000.0000"));
        when(reportAccountRepository.sumLiabilitiesByUserId(userId, List.of(AccountType.CREDIT))).thenReturn(new BigDecimal("1200.0000"));

        NetWorthSnapshotResponse mapped = NetWorthSnapshotResponse.builder()
                .snapshotDate(snapshotDate)
                .totalAssets(new BigDecimal("5000.0000"))
                .totalLiabilities(new BigDecimal("1200.0000"))
                .netWorth(new BigDecimal("3800.0000"))
                .build();
        when(reportMapper.toNetWorthSnapshotResponse(
                        snapshotDate,
                        new BigDecimal("5000.0000"),
                        new BigDecimal("1200.0000"),
                        new BigDecimal("3800.0000")))
                .thenReturn(mapped);

        NetWorthSnapshotResponse result = reportService.getNetWorth(userId, snapshotDate);

        assertEquals(new BigDecimal("3800.0000"), result.getNetWorth());
    }

    @Test
    void getMonthlySummary_throws_whenDateRangeInvalid() {
        assertThrows(InvalidReportDateRangeException.class,
                () -> reportService.getMonthlySummary(userId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 5, 1)));
    }

    @Test
    void getNetWorth_throws_whenSnapshotDateInFuture() {
        assertThrows(InvalidNetWorthSnapshotDateException.class,
                () -> reportService.getNetWorth(userId, TODAY.plusDays(1)));
    }

    @Test
    void exportMonthlySummaryCsv_returnsHeaderAndDataRow() {
        when(reportTransactionRepository.sumAmountByUserIdAndTypeAndTransactionDateBetween(
                        userId, TransactionType.INCOME, from, to))
                .thenReturn(new BigDecimal("1500.0000"));
        when(reportTransactionRepository.sumAmountByUserIdAndTypeAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, from, to))
                .thenReturn(new BigDecimal("500.0000"));

        MonthlySummaryResponse mapped = MonthlySummaryResponse.builder()
                .startDate(from)
                .endDate(to)
                .totalIncome(new BigDecimal("1500.0000"))
                .totalExpense(new BigDecimal("500.0000"))
                .netSavings(new BigDecimal("1000.0000"))
                .build();
        when(reportMapper.toMonthlySummaryResponse(
                        from,
                        to,
                        new BigDecimal("1500.0000"),
                        new BigDecimal("500.0000"),
                        new BigDecimal("1000.0000")))
                .thenReturn(mapped);

        byte[] result = reportService.exportMonthlySummaryCsv(userId, from, to);
        String csv = new String(result, StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("startDate,endDate,totalIncome,totalExpense,netSavings"));
        assertTrue(csv.contains("2026-05-01,2026-05-31,1500.0000,500.0000,1000.0000"));
    }
}
