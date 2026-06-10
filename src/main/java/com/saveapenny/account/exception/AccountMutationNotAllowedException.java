package com.saveapenny.account.exception;

import java.util.UUID;

public class AccountMutationNotAllowedException extends RuntimeException {

    public AccountMutationNotAllowedException(UUID accountId, String fieldName) {
        super("Account %s cannot change %s after it has been used.".formatted(accountId, fieldName));
    }
}
