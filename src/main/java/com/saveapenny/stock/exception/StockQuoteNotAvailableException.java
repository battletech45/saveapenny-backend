package com.saveapenny.stock.exception;

public class StockQuoteNotAvailableException extends RuntimeException {

    public StockQuoteNotAvailableException(String message) {
        super(message);
    }
}
