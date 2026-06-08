package com.saveapenny.goal.simulation;

import java.math.BigDecimal;
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
public class GoalContextSnapshot {

    private String primaryAccountCurrency;
    private BigDecimal averageMonthlyNetIncome;
    private BigDecimal averageMonthlyExpense;
    private boolean missingIncomeHistory;
}
