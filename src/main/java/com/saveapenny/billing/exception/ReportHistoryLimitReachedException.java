package com.saveapenny.billing.exception;

public class ReportHistoryLimitReachedException extends RuntimeException {

    public ReportHistoryLimitReachedException(int maxMonths) {
        super("Free plan reports are limited to the last " + maxMonths + " months of history.");
    }
}
