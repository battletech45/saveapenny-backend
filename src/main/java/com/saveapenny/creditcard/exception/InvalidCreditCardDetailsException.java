package com.saveapenny.creditcard.exception;

public class InvalidCreditCardDetailsException extends RuntimeException {

    public InvalidCreditCardDetailsException(String message) {
        super(message);
    }
}
