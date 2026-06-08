package com.saveapenny.goal.service;

import com.saveapenny.goal.simulation.GoalContextSnapshot;
import java.util.UUID;

public interface GoalContextProvider {

    GoalContextSnapshot getContext(UUID userId);
}
