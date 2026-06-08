package com.saveapenny.goal.simulation;

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
public class SimulationInput {

    private GoalType type;
    private LocalDate asOfDate;
    private String currency;
    private String primaryAccountCurrency;
    private Boolean missingIncomeHistory;
    private Boolean linkedAccountMissing;
    private BigDecimal averageMonthlyNetIncome;
    private BigDecimal averageMonthlyExpense;

    private BigDecimal targetAmount;
    private LocalDate targetDate;
    private BigDecimal monthlyContribution;
    private BigDecimal expectedAnnualReturn;
    private BigDecimal startBalance;

    private BigDecimal currentBalance;
    private BigDecimal apr;
    private BigDecimal minimumPayment;
    private BigDecimal monthlyBudget;
    private LocalDate targetPayoffDate;
    private BigDecimal fixedPayment;

    private BigDecimal targetPrice;
    private BigDecimal downPaymentPercent;
    private BigDecimal currentDownPayment;
    private BigDecimal monthlySaving;
    private BigDecimal expectedPriceInflation;

    private Integer currentAge;
    private Integer targetRetirementAge;
    private BigDecimal currentRetirementSavings;
    private BigDecimal expectedInflation;
    private BigDecimal desiredMonthlyIncomeInRetirement;
    private Integer lifeExpectancy;
    private BigDecimal withdrawalRate;

    private BigDecimal targetMonthlyNetIncome;
    private BigDecimal currentAverageMonthlyNetIncome;
    private BigDecimal expectedIncomeGrowthRate;
    private IncomeStrategy incomeStrategy;
}
