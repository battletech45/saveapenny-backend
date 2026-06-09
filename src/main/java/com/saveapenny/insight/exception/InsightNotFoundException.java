package com.saveapenny.insight.exception;

import java.util.UUID;

public class InsightNotFoundException extends RuntimeException {

    public InsightNotFoundException(UUID insightId) {
        super("Insight not found with id: " + insightId);
    }
}
