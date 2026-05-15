package com.saveapenny.automation.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.automation.entity.RecurringFrequency;
import com.saveapenny.automation.entity.RecurringTransaction;
import com.saveapenny.automation.repository.RecurringTransactionRepository;
import com.saveapenny.automation.service.AutomationDistributedLockService;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionExecutionServiceImplTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AutomationDistributedLockService lockService;

    @InjectMocks
    private RecurringTransactionExecutionServiceImpl executionService;

    @Test
    void processDueRecurringTransactions_skipsWhenLockNotAcquired() {
        when(lockService.tryLock(any())).thenReturn(false);

        executionService.processDueRecurringTransactions(LocalDate.of(2026, 5, 20));

        verify(recurringTransactionRepository, never()).findAllByActiveTrueAndNextRunDateLessThanEqual(any());
        verify(lockService, never()).unlock(any());
    }

    @Test
    void processDueRecurringTransactions_createsTransactionAndAdvancesNextRunDate() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDate nextRunDate = LocalDate.of(2026, 5, 20);

        RecurringTransaction recurring = RecurringTransaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("20.0000"))
                .frequency(RecurringFrequency.WEEKLY)
                .nextRunDate(nextRunDate)
                .active(true)
                .build();

        Account account = Account.builder()
                .id(accountId)
                .userId(userId)
                .currency("USD")
                .active(true)
                .build();

        when(lockService.tryLock(any())).thenReturn(true);
        when(recurringTransactionRepository.findAllByActiveTrueAndNextRunDateLessThanEqual(nextRunDate))
                .thenReturn(List.of(recurring));
        when(accountRepository.findByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(Optional.of(account));

        executionService.processDueRecurringTransactions(nextRunDate);

        verify(transactionService).create(eq(userId), any());
        verify(recurringTransactionRepository).save(recurring);
        verify(lockService).unlock(any());
    }
}
