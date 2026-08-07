package com.saveapenny.creditcard.exception;

public class InvalidCreditCardPaymentException extends RuntimeException {

    public InvalidCreditCardPaymentException(String message) {
        super(message);
    }
}
