package com.saveapenny.transaction.exception;

import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID accountId) {
        super("Insufficient balance for account: " + accountId);
    }
}
