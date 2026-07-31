package com.saveapenny.feedback.exception;

import java.util.UUID;

public class FeedbackNotFoundException extends RuntimeException {

    public FeedbackNotFoundException(UUID feedbackId) {
        super("Feedback not found: " + feedbackId);
    }
}
