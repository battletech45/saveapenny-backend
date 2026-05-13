package com.saveapenny.budget.exception;

import java.time.LocalDate;

public class InvalidBudgetDateRangeException extends RuntimeException {

    public InvalidBudgetDateRangeException(LocalDate startDate, LocalDate endDate) {
        super("Invalid budget date range: startDate=" + startDate + ", endDate=" + endDate);
    }
}
