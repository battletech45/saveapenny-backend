package com.saveapenny.account.exception;

public class InitialBalanceRequiredException extends RuntimeException {

    public InitialBalanceRequiredException() {
        super("initialBalance is required unless type is CREDIT.");
    }
}
