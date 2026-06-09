package com.saveapenny.goal.simulation.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.goal.entity.GoalType;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class ParsedGoalDraft {

    private GoalType type;
    private String title;
    private BigDecimal targetAmount;
    private String currency;
    private LocalDate targetDate;
    private JsonNode inputs;
}
