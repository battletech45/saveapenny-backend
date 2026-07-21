package com.saveapenny.billing.exception;

public class FreePlanLimitReachedException extends RuntimeException {

    public FreePlanLimitReachedException(String resource, int limit) {
        super("Free plan limit reached for " + resource + ": maximum of " + limit + " active " + resource + "(s).");
    }
}
