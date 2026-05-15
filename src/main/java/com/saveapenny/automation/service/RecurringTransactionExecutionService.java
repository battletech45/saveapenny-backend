package com.saveapenny.automation.service;

import java.time.LocalDate;

public interface RecurringTransactionExecutionService {

    void processDueRecurringTransactions(LocalDate runDate);
}
