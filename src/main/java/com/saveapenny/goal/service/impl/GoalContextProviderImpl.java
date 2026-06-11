package com.saveapenny.goal.service.impl;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.goal.simulation.GoalContextSnapshot;
import com.saveapenny.goal.service.GoalContextProvider;
import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GoalContextProviderImpl implements GoalContextProvider {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final Clock assistantClock;

    public GoalContextProviderImpl(AccountRepository accountRepository, TransactionRepository transactionRepository, Clock assistantClock) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.assistantClock = assistantClock;
    }

    @Override
    public GoalContextSnapshot getContext(UUID userId) {
        List<Account> accounts = accountRepository.findAllByUserIdAndActiveTrue(userId);
        LocalDate asOfDate = LocalDate.now(assistantClock);
        LocalDate from = asOfDate.minusMonths(3).withDayOfMonth(1);

        List<Transaction> incomes = transactionRepository.findAllByUserIdAndTypeAndTransactionDateBetween(
                userId,
                TransactionType.INCOME,
                from,
                asOfDate);
        List<Transaction> expenses = transactionRepository.findAllByUserIdAndTypeAndTransactionDateBetween(
                userId,
                TransactionType.EXPENSE,
                from,
                asOfDate);

        BigDecimal totalIncome = incomes.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = expenses.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<YearMonth> incomeMonths = incomes.stream().map(item -> YearMonth.from(item.getTransactionDate())).collect(Collectors.toSet());
        Set<YearMonth> expenseMonths = expenses.stream().map(item -> YearMonth.from(item.getTransactionDate())).collect(Collectors.toSet());

        return GoalContextSnapshot.builder()
                .primaryAccountCurrency(accounts.isEmpty() ? null : accounts.getFirst().getCurrency())
                .averageMonthlyNetIncome(averageForObservedMonths(totalIncome, incomeMonths.size()))
                .averageMonthlyExpense(averageForObservedMonths(totalExpense, expenseMonths.size()))
                .missingIncomeHistory(incomeMonths.size() < 3)
                .build();
    }

    private BigDecimal averageForObservedMonths(BigDecimal totalAmount, int observedMonths) {
        if (observedMonths <= 0 || totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalAmount.divide(BigDecimal.valueOf(observedMonths), java.math.MathContext.DECIMAL64);
    }
}
