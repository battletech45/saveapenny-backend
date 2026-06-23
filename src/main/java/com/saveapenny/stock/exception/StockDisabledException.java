package com.saveapenny.stock.exception;

public class StockDisabledException extends RuntimeException {

    public StockDisabledException(String message) {
        super(message);
    }
}
