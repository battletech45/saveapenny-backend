package com.saveapenny.automation.service.impl;

import com.saveapenny.automation.service.RecurringTransactionExecutionService;
import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionScheduler {

    private final RecurringTransactionExecutionService recurringTransactionExecutionService;

    public RecurringTransactionScheduler(RecurringTransactionExecutionService recurringTransactionExecutionService) {
        this.recurringTransactionExecutionService = recurringTransactionExecutionService;
    }

    @Scheduled(cron = "${automation.recurring.cron:0 */5 * * * *}")
    public void processDueTransactions() {
        recurringTransactionExecutionService.processDueRecurringTransactions(LocalDate.now());
    }
}
