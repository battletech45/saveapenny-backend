package com.saveapenny.account.exception;

import java.util.UUID;

public class AccountInactiveException extends RuntimeException {

    public AccountInactiveException(UUID accountId) {
        super("Account is inactive: " + accountId);
    }
}
