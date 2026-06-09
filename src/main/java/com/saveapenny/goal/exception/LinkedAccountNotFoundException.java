package com.saveapenny.goal.exception;

import java.util.UUID;

public class LinkedAccountNotFoundException extends RuntimeException {

    public LinkedAccountNotFoundException(UUID accountId) {
        super("Linked account not found: " + accountId);
    }
}
