package com.saveapenny.goal.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMin;
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
public class UpdateGoalRequest {

    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    @Size(max = 120)
    private String title;

    @DecimalMin(value = "0.0001")
    private BigDecimal targetAmount;

    @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO currency code")
    private String currency;

    private LocalDate targetDate;

    private UUID linkedAccountId;

    private JsonNode inputs;
}
