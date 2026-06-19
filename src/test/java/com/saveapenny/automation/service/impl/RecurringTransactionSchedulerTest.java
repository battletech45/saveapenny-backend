package com.saveapenny.automation.service.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.automation.service.RecurringTransactionExecutionService;
import com.saveapenny.config.TimeService;
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

    @Mock
    private TimeService timeService;

    private RecurringTransactionScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(timeService.today()).thenReturn(LocalDate.of(2026, 6, 19));
        scheduler = new RecurringTransactionScheduler(recurringTransactionExecutionService, timeService);
    }

    @Test
    void processDueTransactions_delegatesToExecutionService() {
        scheduler.processDueTransactions();

        verify(recurringTransactionExecutionService).processDueRecurringTransactions(LocalDate.of(2026, 6, 19));
    }
}
