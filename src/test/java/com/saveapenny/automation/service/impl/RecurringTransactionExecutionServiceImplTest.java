package com.saveapenny.automation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.analytics.service.AnalyticsEventPublisher;
import com.saveapenny.automation.entity.RecurringExecutionHistory;
import com.saveapenny.automation.entity.RecurringExecutionStatus;
import com.saveapenny.automation.entity.RecurringFrequency;
import com.saveapenny.automation.entity.RecurringStatus;
import com.saveapenny.automation.entity.RecurringTransaction;
import com.saveapenny.automation.repository.RecurringExecutionHistoryRepository;
import com.saveapenny.automation.repository.RecurringTransactionRepository;
import com.saveapenny.automation.service.AutomationDistributedLockService;
import com.saveapenny.transaction.dto.CreateTransactionRequest;
import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.service.TransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionExecutionServiceImplTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private RecurringExecutionHistoryRepository executionHistoryRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AutomationDistributedLockService lockService;

    @Mock
    private AnalyticsEventPublisher analyticsEventPublisher;

    @Captor
    private ArgumentCaptor<RecurringTransaction> recurringTransactionCaptor;

    @Captor
    private ArgumentCaptor<RecurringExecutionHistory> executionHistoryCaptor;

    @InjectMocks
    private RecurringTransactionExecutionServiceImpl executionService;

    @Test
    void processDueRecurringTransactions_skipsWhenLockNotAcquired() {
        when(lockService.tryLock(any())).thenReturn(false);

        executionService.processDueRecurringTransactions(LocalDate.of(2026, 5, 20));

        verify(recurringTransactionRepository, never()).findAllByStatusAndNextRunDateLessThanEqual(any(), any());
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
                .status(RecurringStatus.ACTIVE)
                .build();

        Account account = Account.builder()
                .id(accountId)
                .userId(userId)
                .currency("USD")
                .active(true)
                .build();

        when(lockService.tryLock(any())).thenReturn(true);
        when(recurringTransactionRepository.findAllByStatusAndNextRunDateLessThanEqual(RecurringStatus.ACTIVE, nextRunDate))
                .thenReturn(List.of(recurring));
        when(executionHistoryRepository.findAllByRecurringTransactionIdAndScheduledDate(any(), any()))
                .thenReturn(Collections.emptyList());
        when(accountRepository.findByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(Optional.of(account));
        when(transactionService.create(eq(userId), any(CreateTransactionRequest.class)))
                .thenReturn(TransactionResponse.builder().id(UUID.randomUUID()).build());

        executionService.processDueRecurringTransactions(nextRunDate);

        verify(transactionService).create(eq(userId), any());
        verify(recurringTransactionRepository).save(recurring);
        verify(executionHistoryRepository).save(executionHistoryCaptor.capture());
        RecurringExecutionHistory history = executionHistoryCaptor.getValue();
        assertThat(history.getScheduledDate()).isEqualTo(nextRunDate);
        assertThat(history.getStatus()).isEqualTo(RecurringExecutionStatus.SUCCESS);
        verify(lockService).unlock(any());
    }

    @Test
    void processDueRecurringTransactions_skipsAlreadyExecutedRun() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        LocalDate nextRunDate = LocalDate.of(2026, 5, 20);

        RecurringTransaction recurring = RecurringTransaction.builder()
                .id(recurringId)
                .userId(userId)
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("20.0000"))
                .frequency(RecurringFrequency.WEEKLY)
                .nextRunDate(nextRunDate)
                .status(RecurringStatus.ACTIVE)
                .build();

        RecurringExecutionHistory existingHistory = RecurringExecutionHistory.builder()
                .id(UUID.randomUUID())
                .recurringTransactionId(recurringId)
                .scheduledDate(nextRunDate)
                .status(RecurringExecutionStatus.SUCCESS)
                .build();

        when(lockService.tryLock(any())).thenReturn(true);
        when(recurringTransactionRepository.findAllByStatusAndNextRunDateLessThanEqual(RecurringStatus.ACTIVE, nextRunDate))
                .thenReturn(List.of(recurring));
        when(executionHistoryRepository.findAllByRecurringTransactionIdAndScheduledDate(recurringId, nextRunDate))
                .thenReturn(List.of(existingHistory));

        executionService.processDueRecurringTransactions(nextRunDate);

        verify(transactionService, never()).create(any(), any());
        verify(recurringTransactionRepository).save(recurringTransactionCaptor.capture());
        RecurringTransaction saved = recurringTransactionCaptor.getValue();
        LocalDate expectedNext = LocalDate.of(2026, 5, 27);
        assertThat(saved.getNextRunDate()).isEqualTo(expectedNext);
        verify(lockService).unlock(any());
    }

    @Test
    void processDueRecurringTransactions_catchesUpMissedRuns() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        LocalDate nextRunDate = LocalDate.of(2026, 5, 1);
        LocalDate effectiveRunDate = LocalDate.of(2026, 5, 3);

        RecurringTransaction recurring = RecurringTransaction.builder()
                .id(recurringId)
                .userId(userId)
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("10.0000"))
                .frequency(RecurringFrequency.DAILY)
                .nextRunDate(nextRunDate)
                .status(RecurringStatus.ACTIVE)
                .build();

        Account account = Account.builder()
                .id(accountId)
                .userId(userId)
                .currency("USD")
                .active(true)
                .build();

        when(lockService.tryLock(any())).thenReturn(true);
        when(recurringTransactionRepository.findAllByStatusAndNextRunDateLessThanEqual(RecurringStatus.ACTIVE, effectiveRunDate))
                .thenReturn(List.of(recurring));
        when(executionHistoryRepository.findAllByRecurringTransactionIdAndScheduledDate(any(), any()))
                .thenReturn(Collections.emptyList());
        when(accountRepository.findByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(Optional.of(account));
        when(transactionService.create(eq(userId), any(CreateTransactionRequest.class)))
                .thenReturn(TransactionResponse.builder().id(UUID.randomUUID()).build());

        executionService.processDueRecurringTransactions(effectiveRunDate);

        verify(transactionService, times(3)).create(eq(userId), any());
        verify(recurringTransactionRepository, times(3)).save(any());
        verify(lockService).unlock(any());
    }

    @Test
    void processDueRecurringTransactions_recordsFailureAndDoesNotAdvanceOnException() {
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
                .status(RecurringStatus.ACTIVE)
                .build();

        Account account = Account.builder()
                .id(accountId)
                .userId(userId)
                .currency("USD")
                .active(true)
                .build();

        when(lockService.tryLock(any())).thenReturn(true);
        when(recurringTransactionRepository.findAllByStatusAndNextRunDateLessThanEqual(RecurringStatus.ACTIVE, nextRunDate))
                .thenReturn(List.of(recurring));
        when(executionHistoryRepository.findAllByRecurringTransactionIdAndScheduledDate(any(), any()))
                .thenReturn(Collections.emptyList());
        when(accountRepository.findByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(Optional.of(account));
        when(transactionService.create(eq(userId), any(CreateTransactionRequest.class)))
                .thenThrow(new RuntimeException("Insufficient balance"));

        executionService.processDueRecurringTransactions(nextRunDate);

        verify(transactionService).create(eq(userId), any());
        verify(recurringTransactionRepository, never()).save(recurring);
        verify(executionHistoryRepository).save(executionHistoryCaptor.capture());
        RecurringExecutionHistory history = executionHistoryCaptor.getValue();
        assertThat(history.getScheduledDate()).isEqualTo(nextRunDate);
        assertThat(history.getStatus()).isEqualTo(RecurringExecutionStatus.FAILED);
        verify(lockService).unlock(any());
    }
}
