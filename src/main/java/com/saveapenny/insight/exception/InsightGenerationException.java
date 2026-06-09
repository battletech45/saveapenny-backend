package com.saveapenny.insight.exception;

public class InsightGenerationException extends RuntimeException {

    public InsightGenerationException(String message) {
        super(message);
    }

    public InsightGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
