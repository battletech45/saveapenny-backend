package com.saveapenny.stock.exception;

public class InvalidStockSymbolException extends RuntimeException {

    public InvalidStockSymbolException(String message) {
        super(message);
    }
}
