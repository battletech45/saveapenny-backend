package com.saveapenny.budget.exception;

import java.util.UUID;

public class BudgetNotFoundException extends RuntimeException {

    public BudgetNotFoundException(UUID budgetId) {
        super("Budget not found: " + budgetId);
    }
}
