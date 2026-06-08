package com.saveapenny.budget.exception;

public class BudgetBatchDeleteException extends RuntimeException {

    public BudgetBatchDeleteException(int expected, int deleted) {
        super("Batch delete: expected " + expected + " budgets, but only " + deleted + " were deleted. "
                + "Some budgets were not found or do not belong to the user.");
    }
}
