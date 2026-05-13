package com.saveapenny.budget.dto;

import com.saveapenny.budget.entity.BudgetPeriod;
import jakarta.validation.constraints.NotNull;
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
public class UpdateBudgetRequest {

    @NotNull
    private UUID categoryId;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private BudgetPeriod period;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
