package com.saveapenny.goal.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalRunTrigger;
import java.time.OffsetDateTime;
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
public class GoalRunResponse {

    private UUID id;
    private UUID goalId;
    private UUID scenarioId;
    private JsonNode inputsSnapshot;
    private JsonNode outputSummary;
    private JsonNode outputSeries;
    private Feasibility feasibility;
    private GoalRunTrigger triggeredBy;
    private OffsetDateTime createdAt;
}
