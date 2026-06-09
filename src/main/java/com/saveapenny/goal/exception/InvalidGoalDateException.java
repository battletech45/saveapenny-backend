package com.saveapenny.goal.exception;

import java.time.LocalDate;

public class InvalidGoalDateException extends RuntimeException {

    public InvalidGoalDateException(LocalDate targetDate) {
        super("Goal target date must be in the future: " + targetDate);
    }
}
