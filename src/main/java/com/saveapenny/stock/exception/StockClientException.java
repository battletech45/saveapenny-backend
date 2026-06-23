package com.saveapenny.stock.exception;

public class StockClientException extends RuntimeException {

    public StockClientException(String message) {
        super(message);
    }

    public StockClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
