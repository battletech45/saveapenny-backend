package com.saveapenny.goal.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.goal.entity.GoalType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class CreateGoalRequest {

    @NotNull
    private GoalType type;

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotNull
    @DecimalMin(value = "0.0001")
    private BigDecimal targetAmount;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO currency code")
    private String currency;

    @NotNull
    private LocalDate targetDate;

    private UUID linkedAccountId;

    @NotNull
    private JsonNode inputs;
}
