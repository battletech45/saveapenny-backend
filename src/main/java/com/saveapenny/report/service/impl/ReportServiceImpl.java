package com.saveapenny.report.service.impl;

import com.saveapenny.account.entity.AccountType;
import com.saveapenny.analytics.dto.AnalyticsEvent;
import com.saveapenny.analytics.service.AnalyticsEventPublisher;
import com.saveapenny.config.TimeService;
import com.saveapenny.report.dto.CashFlowPointResponse;
import java.util.List;
import java.util.Set;
import com.saveapenny.report.dto.CategorySpendingResponse;
import com.saveapenny.report.dto.MonthlySummaryResponse;
import com.saveapenny.report.dto.NetWorthSnapshotResponse;
import com.saveapenny.report.entity.NetWorthSnapshot;
import com.saveapenny.report.exception.InvalidNetWorthSnapshotDateException;
import com.saveapenny.report.exception.InvalidReportDateRangeException;
import com.saveapenny.report.mapper.ReportMapper;
import com.saveapenny.report.repository.NetWorthSnapshotRepository;
import com.saveapenny.report.repository.ReportAccountRepository;
import com.saveapenny.report.repository.ReportTransactionRepository;
import com.saveapenny.report.service.ReportService;
import com.saveapenny.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final List<AccountType> LIABILITY_ACCOUNT_TYPES = List.of(AccountType.CREDIT);

    private final ReportTransactionRepository reportTransactionRepository;
    private final ReportAccountRepository reportAccountRepository;
    private final NetWorthSnapshotRepository netWorthSnapshotRepository;
    private final ReportMapper reportMapper;
    private final TimeService timeService;
    private final AnalyticsEventPublisher analyticsEventPublisher;

    public ReportServiceImpl(
            ReportTransactionRepository reportTransactionRepository,
            ReportAccountRepository reportAccountRepository,
            NetWorthSnapshotRepository netWorthSnapshotRepository,
            ReportMapper reportMapper,
            TimeService timeService,
            AnalyticsEventPublisher analyticsEventPublisher) {
        this.reportTransactionRepository = reportTransactionRepository;
        this.reportAccountRepository = reportAccountRepository;
        this.netWorthSnapshotRepository = netWorthSnapshotRepository;
        this.reportMapper = reportMapper;
        this.timeService = timeService;
        this.analyticsEventPublisher = analyticsEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(UUID currentUserId, LocalDate from, LocalDate to) {
        validateDateRange(from, to);

        BigDecimal totalIncome = nullSafeAmount(reportTransactionRepository
                .sumAmountByUserIdAndTypeAndTransactionDateBetween(currentUserId, TransactionType.INCOME, from, to));
        BigDecimal totalExpense = nullSafeAmount(reportTransactionRepository
                .sumAmountByUserIdAndTypeAndTransactionDateBetween(currentUserId, TransactionType.EXPENSE, from, to));
        BigDecimal netSavings = totalIncome.subtract(totalExpense);

        return reportMapper.toMonthlySummaryResponse(from, to, totalIncome, totalExpense, netSavings);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportMonthlySummaryCsv(UUID currentUserId, LocalDate from, LocalDate to) {
        MonthlySummaryResponse summary = getMonthlySummary(currentUserId, from, to);
        String csv = String.join(
                "\n",
                "startDate,endDate,totalIncome,totalExpense,netSavings",
                "%s,%s,%s,%s,%s".formatted(
                        summary.getStartDate(),
                        summary.getEndDate(),
                        summary.getTotalIncome(),
                        summary.getTotalExpense(),
                        summary.getNetSavings()));
        analyticsEventPublisher.publish(new AnalyticsEvent(
                "report_generated",
                Map.of("report_type", "monthly_summary_csv")));
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorySpendingResponse> getCategorySpending(UUID currentUserId, LocalDate from, LocalDate to) {
        validateDateRange(from, to);

        BigDecimal totalExpense = nullSafeAmount(reportTransactionRepository
                .sumAmountByUserIdAndTypeAndTransactionDateBetween(currentUserId, TransactionType.EXPENSE, from, to));

        return reportTransactionRepository.findCategorySpendingByUserIdAndTransactionDateBetween(currentUserId, from, to)
                .stream()
                .map(reportMapper::toCategorySpendingResponse)
                .map(item -> withUsagePercentage(item, totalExpense))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashFlowPointResponse> getCashFlow(UUID currentUserId, LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return reportTransactionRepository.findCashFlowByUserIdAndTransactionDateBetween(currentUserId, from, to)
                .stream()
                .map(reportMapper::toCashFlowPointResponse)
                .toList();
    }

    @Override
    @Transactional
    public NetWorthSnapshotResponse getNetWorth(UUID currentUserId, LocalDate snapshotDate) {
        validateSnapshotDate(snapshotDate);

        var existing = netWorthSnapshotRepository.findByUserIdAndSnapshotDate(currentUserId, snapshotDate);
        if (existing.isPresent()) {
            return reportMapper.toNetWorthSnapshotResponse(existing.get());
        }

        BigDecimal totalAssets = nullSafeAmount(reportAccountRepository.sumAssetsByUserId(currentUserId, LIABILITY_ACCOUNT_TYPES));
        BigDecimal totalLiabilities = nullSafeAmount(
                reportAccountRepository.sumLiabilitiesByUserId(currentUserId, LIABILITY_ACCOUNT_TYPES));
        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);

        NetWorthSnapshot snapshot = NetWorthSnapshot.builder()
                .userId(currentUserId)
                .snapshotDate(snapshotDate)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .netWorth(netWorth)
                .build();
        netWorthSnapshotRepository.save(snapshot);

        return reportMapper.toNetWorthSnapshotResponse(snapshotDate, totalAssets, totalLiabilities, netWorth);
    }

    private CategorySpendingResponse withUsagePercentage(CategorySpendingResponse item, BigDecimal totalExpense) {
        BigDecimal usagePercentage = calculateUsagePercentage(item.getTotalAmount(), totalExpense);
        item.setUsagePercentage(usagePercentage);
        return item;
    }

    private BigDecimal calculateUsagePercentage(BigDecimal partialAmount, BigDecimal totalAmount) {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return nullSafeAmount(partialAmount)
                .multiply(HUNDRED)
                .divide(totalAmount, 2, java.math.RoundingMode.HALF_UP);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new InvalidReportDateRangeException(from, to);
        }
    }

    private void validateSnapshotDate(LocalDate snapshotDate) {
        if (snapshotDate == null || snapshotDate.isAfter(timeService.today())) {
            throw new InvalidNetWorthSnapshotDateException(snapshotDate);
        }
    }

    private BigDecimal nullSafeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
