package com.saveapenny.automation.service.impl;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.automation.entity.RecurringExecutionHistory;
import com.saveapenny.automation.entity.RecurringExecutionStatus;
import com.saveapenny.automation.entity.RecurringFrequency;
import com.saveapenny.automation.entity.RecurringStatus;
import com.saveapenny.automation.entity.RecurringTransaction;
import com.saveapenny.automation.repository.RecurringExecutionHistoryRepository;
import com.saveapenny.automation.repository.RecurringTransactionRepository;
import com.saveapenny.automation.service.AutomationDistributedLockService;
import com.saveapenny.automation.service.RecurringTransactionExecutionService;
import com.saveapenny.config.TimeService;
import com.saveapenny.transaction.dto.CreateTransactionRequest;
import com.saveapenny.transaction.service.TransactionService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringTransactionExecutionServiceImpl implements RecurringTransactionExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionExecutionServiceImpl.class);
    private static final String RECURRING_LOCK_NAME = "automation:recurring-transactions";

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final RecurringExecutionHistoryRepository executionHistoryRepository;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final AutomationDistributedLockService lockService;
    private final TimeService timeService;

    public RecurringTransactionExecutionServiceImpl(
            RecurringTransactionRepository recurringTransactionRepository,
            RecurringExecutionHistoryRepository executionHistoryRepository,
            TransactionService transactionService,
            AccountRepository accountRepository,
            AutomationDistributedLockService lockService,
            TimeService timeService) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.executionHistoryRepository = executionHistoryRepository;
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
        this.lockService = lockService;
        this.timeService = timeService;
    }

    @Override
    @Transactional
    public void processDueRecurringTransactions(LocalDate runDate) {
        if (!lockService.tryLock(RECURRING_LOCK_NAME)) {
            return;
        }

        try {
            LocalDate effectiveRunDate = runDate == null ? timeService.today() : runDate;
            List<RecurringTransaction> dueTransactions =
                    recurringTransactionRepository.findAllByStatusAndNextRunDateLessThanEqual(RecurringStatus.ACTIVE, effectiveRunDate);

            for (RecurringTransaction recurringTransaction : dueTransactions) {
                processSingleRecurringTransaction(recurringTransaction, effectiveRunDate);
            }
        } finally {
            lockService.unlock(RECURRING_LOCK_NAME);
        }
    }

    private void processSingleRecurringTransaction(RecurringTransaction recurringTransaction, LocalDate effectiveRunDate) {
        while (!recurringTransaction.getNextRunDate().isAfter(effectiveRunDate)) {
            LocalDate currentRun = recurringTransaction.getNextRunDate();

            if (isAlreadyExecuted(recurringTransaction.getId(), currentRun)) {
                advanceNextRunDate(recurringTransaction);
                recurringTransactionRepository.save(recurringTransaction);
                continue;
            }

            boolean advanced = executeRun(recurringTransaction, currentRun);
            if (!advanced) {
                break;
            }
        }
    }

    private boolean executeRun(RecurringTransaction recurringTransaction, LocalDate currentRun) {
        try {
            Account account = accountRepository
                    .findByIdAndUserIdAndActiveTrue(recurringTransaction.getAccountId(), recurringTransaction.getUserId())
                    .orElse(null);
            if (account == null) {
                recordHistory(recurringTransaction, currentRun, RecurringExecutionStatus.SKIPPED,
                        null, "Account is missing or inactive");
                log.warn("Skipping recurring transaction {} because account is missing or inactive", recurringTransaction.getId());
                return false;
            }

            if (recurringTransaction.getEndDate() != null
                    && currentRun.isAfter(recurringTransaction.getEndDate())) {
                recurringTransaction.setStatus(RecurringStatus.EXPIRED);
                recurringTransactionRepository.save(recurringTransaction);
                return false;
            }

            String description = recurringTransaction.getDescription() != null
                    ? recurringTransaction.getDescription()
                    : "Recurring transaction";

            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .accountId(recurringTransaction.getAccountId())
                    .categoryId(recurringTransaction.getCategoryId())
                    .type(recurringTransaction.getType())
                    .amount(recurringTransaction.getAmount())
                    .currency(account.getCurrency())
                    .description(description)
                    .transactionDate(currentRun)
                    .build();

            var transactionResponse = transactionService.create(recurringTransaction.getUserId(), request);

            recurringTransaction.setLastRunAt(OffsetDateTime.now());
            advanceNextRunDate(recurringTransaction);
            recurringTransactionRepository.save(recurringTransaction);

            recordHistory(recurringTransaction, currentRun, RecurringExecutionStatus.SUCCESS,
                    transactionResponse.getId(), null);
            return true;
        } catch (RuntimeException ex) {
            recordHistory(recurringTransaction, currentRun, RecurringExecutionStatus.FAILED,
                    null, ex.getMessage());
            log.warn("Failed to process recurring transaction {}: {}", recurringTransaction.getId(), ex.getMessage());
            return false;
        }
    }

    private boolean isAlreadyExecuted(UUID recurringTransactionId, LocalDate scheduledDate) {
        List<RecurringExecutionHistory> existing =
                executionHistoryRepository.findAllByRecurringTransactionIdAndScheduledDate(recurringTransactionId, scheduledDate);
        return existing.stream().anyMatch(h -> h.getStatus() == RecurringExecutionStatus.SUCCESS);
    }

    private void advanceNextRunDate(RecurringTransaction recurringTransaction) {
        LocalDate next = nextRunDate(recurringTransaction.getNextRunDate(), recurringTransaction.getFrequency());
        recurringTransaction.setNextRunDate(next);
    }

    private void recordHistory(RecurringTransaction recurringTransaction,
                               LocalDate scheduledDate,
                               RecurringExecutionStatus status,
                               UUID transactionId,
                               String failureReason) {
        RecurringExecutionHistory history = RecurringExecutionHistory.builder()
                .recurringTransactionId(recurringTransaction.getId())
                .userId(recurringTransaction.getUserId())
                .status(status)
                .scheduledDate(scheduledDate)
                .transactionId(transactionId)
                .failureReason(failureReason)
                .build();
        executionHistoryRepository.save(history);
    }

    private LocalDate nextRunDate(LocalDate current, RecurringFrequency frequency) {
        return switch (frequency) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case YEARLY -> current.plusYears(1);
        };
    }
}
