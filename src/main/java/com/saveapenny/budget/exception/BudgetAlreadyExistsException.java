package com.saveapenny.budget.exception;

public class BudgetAlreadyExistsException extends RuntimeException {

    public BudgetAlreadyExistsException() {
        super("A budget already exists for the selected category and period.");
    }
}
