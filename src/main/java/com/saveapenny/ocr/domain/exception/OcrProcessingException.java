package com.saveapenny.ocr.domain.exception;

public class OcrProcessingException extends RuntimeException {

    public OcrProcessingException(String message) {
        super(message);
    }

    public OcrProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
