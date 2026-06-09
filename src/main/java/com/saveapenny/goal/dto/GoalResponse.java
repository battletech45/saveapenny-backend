package com.saveapenny.goal.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class GoalResponse {

    private UUID id;
    private GoalType type;
    private String title;
    private BigDecimal targetAmount;
    private String currency;
    private LocalDate targetDate;
    private UUID linkedAccountId;
    private GoalStatus status;
    private JsonNode inputs;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
