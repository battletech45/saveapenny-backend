package com.saveapenny.goal.exception;

import java.util.UUID;

public class ScenarioNotFoundException extends RuntimeException {

    public ScenarioNotFoundException(UUID scenarioId) {
        super("Scenario not found: " + scenarioId);
    }
}
