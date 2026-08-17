package com.saveapenny.creditcard.exception;

import java.util.UUID;

public class CreditCardDetailsNotFoundException extends RuntimeException {

    public CreditCardDetailsNotFoundException(UUID accountId) {
        super("Credit card details not found for account: " + accountId);
    }
}
