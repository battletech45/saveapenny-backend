package com.saveapenny.goal.exception;

import com.saveapenny.goal.entity.GoalType;

public class InvalidGoalTypeException extends RuntimeException {

    public InvalidGoalTypeException(GoalType expectedType, String actualType) {
        super("Goal input type mismatch. Expected " + expectedType + " but got " + actualType);
    }
}
