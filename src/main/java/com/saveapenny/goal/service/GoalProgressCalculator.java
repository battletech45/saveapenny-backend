package com.saveapenny.goal.service;

import java.time.LocalDate;
import java.util.UUID;

public interface GoalProgressCalculator {

    GoalProgressReport calculate(UUID userId, UUID goalId, LocalDate asOf);
}
