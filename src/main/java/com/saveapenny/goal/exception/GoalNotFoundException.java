package com.saveapenny.goal.exception;

import java.util.UUID;

public class GoalNotFoundException extends RuntimeException {

    public GoalNotFoundException(UUID goalId) {
        super("Goal not found: " + goalId);
    }
}
