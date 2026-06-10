package com.saveapenny.automation.service.impl;

import static org.mockito.Mockito.verify;

import com.saveapenny.automation.service.RecurringTransactionExecutionService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionSchedulerTest {

    @Mock
    private RecurringTransactionExecutionService recurringTransactionExecutionService;

    private RecurringTransactionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RecurringTransactionScheduler(recurringTransactionExecutionService);
    }

    @Test
    void processDueTransactions_delegatesToExecutionService() {
        scheduler.processDueTransactions();

        verify(recurringTransactionExecutionService).processDueRecurringTransactions(LocalDate.now());
    }
}
