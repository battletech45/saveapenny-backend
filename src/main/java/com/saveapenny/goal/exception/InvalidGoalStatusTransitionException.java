package com.saveapenny.goal.exception;

import com.saveapenny.goal.entity.GoalStatus;
import java.util.UUID;

public class InvalidGoalStatusTransitionException extends RuntimeException {

    public InvalidGoalStatusTransitionException(UUID goalId, GoalStatus currentStatus, GoalStatus nextStatus) {
        super("Invalid goal status transition for " + goalId + ": " + currentStatus + " -> " + nextStatus);
    }
}
