package com.saveapenny.goal.simulation.dto;

import com.saveapenny.goal.simulation.SimulationResult;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalSimulationResponse {

    private UUID goalId;
    private ParsedGoalDraft parsedGoal;
    private SimulationResult result;
    private String narrative;
    private String disclaimer;
    private boolean draft;
}
