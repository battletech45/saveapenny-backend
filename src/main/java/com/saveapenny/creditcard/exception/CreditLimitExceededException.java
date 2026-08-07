package com.saveapenny.creditcard.exception;

import java.util.UUID;

public class CreditLimitExceededException extends RuntimeException {

    public CreditLimitExceededException(UUID accountId) {
        super("Credit limit exceeded for account: " + accountId);
    }
}
