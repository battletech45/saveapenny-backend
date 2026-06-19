package com.saveapenny.automation.service.impl;

import com.saveapenny.automation.service.RecurringTransactionExecutionService;
import com.saveapenny.config.TimeService;
import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionScheduler {

    private final RecurringTransactionExecutionService recurringTransactionExecutionService;
    private final TimeService timeService;

    public RecurringTransactionScheduler(RecurringTransactionExecutionService recurringTransactionExecutionService, TimeService timeService) {
        this.recurringTransactionExecutionService = recurringTransactionExecutionService;
        this.timeService = timeService;
    }

    @Scheduled(cron = "${automation.recurring.cron:0 */5 * * * *}")
    public void processDueTransactions() {
        recurringTransactionExecutionService.processDueRecurringTransactions(timeService.today());
    }
}
