package com.saveapenny.automation.exception;

import com.saveapenny.automation.entity.RecurringStatus;
import java.util.UUID;

public class InvalidRecurringTransactionStatusTransitionException extends RuntimeException {

    public InvalidRecurringTransactionStatusTransitionException(UUID id, RecurringStatus current, RecurringStatus desired) {
        super("Cannot transition recurring transaction " + id + " from " + current + " to " + desired);
    }
}
