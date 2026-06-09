package com.saveapenny.goal.dto;

import com.fasterxml.jackson.databind.JsonNode;
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
public class ScenarioResponse {

    private UUID id;
    private UUID goalId;
    private String name;
    private JsonNode inputs;
    private Boolean isBaseline;
    private OffsetDateTime createdAt;
}
