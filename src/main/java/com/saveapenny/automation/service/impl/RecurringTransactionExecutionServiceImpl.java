package com.saveapenny.automation.service.impl;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.automation.entity.RecurringFrequency;
import com.saveapenny.automation.entity.RecurringTransaction;
import com.saveapenny.automation.repository.RecurringTransactionRepository;
import com.saveapenny.automation.service.AutomationDistributedLockService;
import com.saveapenny.automation.service.RecurringTransactionExecutionService;
import com.saveapenny.transaction.dto.CreateTransactionRequest;
import com.saveapenny.transaction.service.TransactionService;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringTransactionExecutionServiceImpl implements RecurringTransactionExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionExecutionServiceImpl.class);
    private static final String RECURRING_LOCK_NAME = "automation:recurring-transactions";

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final AutomationDistributedLockService lockService;

    public RecurringTransactionExecutionServiceImpl(
            RecurringTransactionRepository recurringTransactionRepository,
            TransactionService transactionService,
            AccountRepository accountRepository,
            AutomationDistributedLockService lockService) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
        this.lockService = lockService;
    }

    @Override
    @Transactional
    public void processDueRecurringTransactions(LocalDate runDate) {
        if (!lockService.tryLock(RECURRING_LOCK_NAME)) {
            return;
        }

        try {
            LocalDate effectiveRunDate = runDate == null ? LocalDate.now() : runDate;
            List<RecurringTransaction> dueTransactions =
                    recurringTransactionRepository.findAllByActiveTrueAndNextRunDateLessThanEqual(effectiveRunDate);

            for (RecurringTransaction recurringTransaction : dueTransactions) {
                processSingleRecurringTransaction(recurringTransaction);
            }
        } finally {
            lockService.unlock(RECURRING_LOCK_NAME);
        }
    }

    private void processSingleRecurringTransaction(RecurringTransaction recurringTransaction) {
        try {
            Account account = accountRepository
                    .findByIdAndUserIdAndActiveTrue(recurringTransaction.getAccountId(), recurringTransaction.getUserId())
                    .orElse(null);
            if (account == null) {
                log.warn("Skipping recurring transaction {} because account is missing or inactive", recurringTransaction.getId());
                return;
            }

            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .accountId(recurringTransaction.getAccountId())
                    .categoryId(recurringTransaction.getCategoryId())
                    .type(recurringTransaction.getType())
                    .amount(recurringTransaction.getAmount())
                    .currency(account.getCurrency())
                    .description("Recurring transaction")
                    .transactionDate(recurringTransaction.getNextRunDate())
                    .build();

            transactionService.create(recurringTransaction.getUserId(), request);

            recurringTransaction.setNextRunDate(nextRunDate(recurringTransaction.getNextRunDate(), recurringTransaction.getFrequency()));
            recurringTransactionRepository.save(recurringTransaction);
        } catch (RuntimeException ex) {
            log.warn("Failed to process recurring transaction {}: {}", recurringTransaction.getId(), ex.getMessage());
        }
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
