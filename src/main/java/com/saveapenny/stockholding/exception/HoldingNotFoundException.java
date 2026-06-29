package com.saveapenny.stockholding.exception;

public class HoldingNotFoundException extends RuntimeException {

    public HoldingNotFoundException(String message) {
        super(message);
    }
}
