package com.saveapenny.report.service.impl;

import com.saveapenny.account.entity.AccountType;
import com.saveapenny.report.dto.CashFlowPointResponse;
import com.saveapenny.report.dto.CategorySpendingResponse;
import com.saveapenny.report.dto.MonthlySummaryResponse;
import com.saveapenny.report.dto.NetWorthSnapshotResponse;
import com.saveapenny.report.exception.InvalidNetWorthSnapshotDateException;
import com.saveapenny.report.exception.InvalidReportDateRangeException;
import com.saveapenny.report.mapper.ReportMapper;
import com.saveapenny.report.repository.ReportAccountRepository;
import com.saveapenny.report.repository.ReportTransactionRepository;
import com.saveapenny.report.service.ReportService;
import com.saveapenny.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ReportTransactionRepository reportTransactionRepository;
    private final ReportAccountRepository reportAccountRepository;
    private final ReportMapper reportMapper;

    public ReportServiceImpl(
            ReportTransactionRepository reportTransactionRepository,
            ReportAccountRepository reportAccountRepository,
            ReportMapper reportMapper) {
        this.reportTransactionRepository = reportTransactionRepository;
        this.reportAccountRepository = reportAccountRepository;
        this.reportMapper = reportMapper;
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
    @Transactional(readOnly = true)
    public NetWorthSnapshotResponse getNetWorth(UUID currentUserId, LocalDate snapshotDate) {
        validateSnapshotDate(snapshotDate);

        BigDecimal totalAssets = nullSafeAmount(reportAccountRepository.sumAssetsByUserId(currentUserId, AccountType.CREDIT));
        BigDecimal totalLiabilities = nullSafeAmount(
                reportAccountRepository.sumLiabilitiesByUserId(currentUserId, AccountType.CREDIT));
        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);

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
        if (snapshotDate == null || snapshotDate.isAfter(LocalDate.now())) {
            throw new InvalidNetWorthSnapshotDateException(snapshotDate);
        }
    }

    private BigDecimal nullSafeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
