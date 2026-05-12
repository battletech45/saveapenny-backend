package com.saveapenny.account.exception;

public class AccountNameAlreadyExistsException extends RuntimeException {

    public AccountNameAlreadyExistsException(String accountName) {
        super("Account name is already in use: " + accountName);
    }
}
