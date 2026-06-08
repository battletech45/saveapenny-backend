package com.saveapenny.budget.dto;

import com.saveapenny.budget.entity.BudgetPeriod;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBudgetRequest {

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
