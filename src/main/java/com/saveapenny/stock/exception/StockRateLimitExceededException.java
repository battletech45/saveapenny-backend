package com.saveapenny.stock.exception;

public class StockRateLimitExceededException extends RuntimeException {

    public StockRateLimitExceededException(String message) {
        super(message);
    }
}
