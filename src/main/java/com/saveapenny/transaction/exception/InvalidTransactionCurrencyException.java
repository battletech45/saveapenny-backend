package com.saveapenny.transaction.exception;

import java.util.UUID;

public class InvalidTransactionCurrencyException extends RuntimeException {

    public InvalidTransactionCurrencyException(UUID accountId, String accountCurrency, String requestCurrency) {
        super("Transaction currency must match account currency for account %s. Expected %s but received %s."
                .formatted(accountId, accountCurrency, requestCurrency));
    }
}
