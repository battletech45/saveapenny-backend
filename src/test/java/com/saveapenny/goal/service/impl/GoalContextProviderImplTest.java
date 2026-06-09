package com.saveapenny.goal.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.goal.simulation.GoalContextSnapshot;
import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalContextProviderImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private Clock assistantClock;

    @InjectMocks
    private GoalContextProviderImpl goalContextProvider;

    private UUID userId;
    private LocalDate fixedNow;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        fixedNow = LocalDate.of(2026, 6, 15);
        when(assistantClock.instant()).thenReturn(Instant.parse("2026-06-15T10:00:00Z"));
        when(assistantClock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void getContext_returnsSnapshotWithAccountsAndTransactions() {
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .currency("USD")
                .active(true)
                .build();

        Transaction income1 = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("3000"))
                .transactionDate(LocalDate.of(2026, 5, 1))
                .build();
        Transaction income2 = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("3000"))
                .transactionDate(LocalDate.of(2026, 6, 1))
                .build();
        Transaction income3 = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("3000"))
                .transactionDate(LocalDate.of(2026, 4, 1))
                .build();

        Transaction expense1 = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("1500"))
                .transactionDate(LocalDate.of(2026, 5, 15))
                .build();
        Transaction expense2 = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("1500"))
                .transactionDate(LocalDate.of(2026, 6, 10))
                .build();

        when(accountRepository.findAllByUserIdAndActiveTrue(userId)).thenReturn(List.of(account));
        when(transactionRepository.findAllByUserIdAndTypeAndTransactionDateBetween(
                userId, TransactionType.INCOME, LocalDate.of(2026, 3, 1), fixedNow))
                .thenReturn(List.of(income1, income2, income3));
        when(transactionRepository.findAllByUserIdAndTypeAndTransactionDateBetween(
                userId, TransactionType.EXPENSE, LocalDate.of(2026, 3, 1), fixedNow))
                .thenReturn(List.of(expense1, expense2));

        GoalContextSnapshot snapshot = goalContextProvider.getContext(userId);

        assertNotNull(snapshot);
        assertEquals("USD", snapshot.getPrimaryAccountCurrency());
        assertEquals(0, new BigDecimal("3000").compareTo(snapshot.getAverageMonthlyNetIncome()));
        assertEquals(0, new BigDecimal("1000").compareTo(snapshot.getAverageMonthlyExpense()));
        assertFalse(snapshot.isMissingIncomeHistory());
    }

    @Test
    void getContext_marksMissingIncomeHistoryWhenLessThanThreeMonths() {
        Transaction income1 = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("3000"))
                .transactionDate(LocalDate.of(2026, 6, 1))
                .build();

        when(accountRepository.findAllByUserIdAndActiveTrue(userId)).thenReturn(List.of());
        when(transactionRepository.findAllByUserIdAndTypeAndTransactionDateBetween(
                userId, TransactionType.INCOME, LocalDate.of(2026, 3, 1), fixedNow))
                .thenReturn(List.of(income1));
        when(transactionRepository.findAllByUserIdAndTypeAndTransactionDateBetween(
                userId, TransactionType.EXPENSE, LocalDate.of(2026, 3, 1), fixedNow))
                .thenReturn(List.of());

        GoalContextSnapshot snapshot = goalContextProvider.getContext(userId);

        assertNull(snapshot.getPrimaryAccountCurrency());
        assertTrue(snapshot.isMissingIncomeHistory());
    }

    @Test
    void getContext_withNoAccountsAndNoTransactions_returnsDefaults() {
        when(accountRepository.findAllByUserIdAndActiveTrue(userId)).thenReturn(List.of());
        when(transactionRepository.findAllByUserIdAndTypeAndTransactionDateBetween(
                userId, TransactionType.INCOME, LocalDate.of(2026, 3, 1), fixedNow))
                .thenReturn(List.of());
        when(transactionRepository.findAllByUserIdAndTypeAndTransactionDateBetween(
                userId, TransactionType.EXPENSE, LocalDate.of(2026, 3, 1), fixedNow))
                .thenReturn(List.of());

        GoalContextSnapshot snapshot = goalContextProvider.getContext(userId);

        assertNull(snapshot.getPrimaryAccountCurrency());
        assertEquals(0, BigDecimal.ZERO.compareTo(snapshot.getAverageMonthlyNetIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(snapshot.getAverageMonthlyExpense()));
        assertTrue(snapshot.isMissingIncomeHistory());
    }
}
