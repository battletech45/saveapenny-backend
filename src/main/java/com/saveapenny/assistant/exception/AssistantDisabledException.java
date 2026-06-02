package com.saveapenny.assistant.exception;

public class AssistantDisabledException extends RuntimeException {

    public AssistantDisabledException() {
        super("Assistant feature is disabled.");
    }
}
