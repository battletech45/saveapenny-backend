package com.saveapenny.goal.simulation.strategy;

import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.simulation.AssumptionSet;
import com.saveapenny.goal.simulation.IncomeStrategy;
import com.saveapenny.goal.simulation.SimulationInput;
import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.goal.simulation.SimulationWarning;
import com.saveapenny.goal.simulation.math.SimulationMath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class AbstractGoalSimulationStrategy implements GoalSimulationStrategy {

    protected SimulationResult newResult(GoalType type, SimulationInput input, int horizonMonths) {
        return SimulationResult.builder()
                .version(1)
                .type(type)
                .asOf(SimulationMath.asStartOfDayUtc(resolveAsOfDate(input)))
                .horizonMonths(horizonMonths)
                .currency(input.getCurrency())
                .summary(new LinkedHashMap<>())
                .assumptions(AssumptionSet.builder().values(new LinkedHashMap<>()).build())
                .warnings(commonWarnings(input, horizonMonths))
                .series(new ArrayList<>())
                .build();
    }

    protected LocalDate resolveAsOfDate(SimulationInput input) {
        return input.getAsOfDate() == null ? LocalDate.now() : input.getAsOfDate();
    }

    protected int horizonMonths(LocalDate asOfDate, LocalDate targetDate) {
        return Math.max(SimulationMath.monthsBetween(asOfDate, targetDate), 0);
    }

    protected void putCommonAssumptions(SimulationInput input, SimulationResult result, BigDecimal expectedAnnualReturn, BigDecimal startBalance) {
        Map<String, Object> assumptions = result.getAssumptions().getValues();
        assumptions.put("expectedAnnualReturn", SimulationMath.money(expectedAnnualReturn));
        assumptions.put("startBalance", SimulationMath.money(startBalance));
        assumptions.put("averageMonthlyNetIncome", SimulationMath.money(input.getAverageMonthlyNetIncome()));
        assumptions.put("averageMonthlyExpense", SimulationMath.money(input.getAverageMonthlyExpense()));
    }

    protected Feasibility classifyByIncomeBands(BigDecimal requiredAmount, BigDecimal averageMonthlyIncome, BigDecimal tightFloor, BigDecimal riskFloor, BigDecimal infeasibleFloor) {
        BigDecimal income = SimulationMath.defaulted(averageMonthlyIncome);
        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return requiredAmount.compareTo(BigDecimal.ZERO) > 0 ? Feasibility.INFEASIBLE : Feasibility.ON_TRACK;
        }

        BigDecimal ratio = requiredAmount.divide(income, SimulationMath.MATH_CONTEXT);
        if (ratio.compareTo(infeasibleFloor) > 0) {
            return Feasibility.INFEASIBLE;
        }
        if (ratio.compareTo(riskFloor) >= 0) {
            return Feasibility.AT_RISK;
        }
        if (ratio.compareTo(tightFloor) >= 0) {
            return Feasibility.TIGHT;
        }
        return Feasibility.ON_TRACK;
    }

    protected List<SimulationWarning> commonWarnings(SimulationInput input, int horizonMonths) {
        List<SimulationWarning> warnings = new ArrayList<>();
        if (input.getPrimaryAccountCurrency() != null
                && input.getCurrency() != null
                && !input.getCurrency().equalsIgnoreCase(input.getPrimaryAccountCurrency())) {
            warnings.add(warning("MULTI_CURRENCY", "Goal currency differs from primary account currency. No FX conversion was applied."));
        }
        if (Boolean.TRUE.equals(input.getMissingIncomeHistory())) {
            warnings.add(warning("MISSING_INCOME_HISTORY", "User has fewer than 3 months of income history. Using fallback assumptions."));
        }
        if (Boolean.TRUE.equals(input.getLinkedAccountMissing())) {
            warnings.add(warning("MISSING_LINKED_ACCOUNT", "Linked account was not found at run time."));
        }
        if (SimulationMath.defaulted(input.getAverageMonthlyExpense()).compareTo(SimulationMath.defaulted(input.getAverageMonthlyNetIncome())) > 0) {
            warnings.add(warning("NEGATIVE_CASH_FLOW", "Average monthly expense exceeds average monthly income."));
        }
        if (horizonMonths > 480) {
            warnings.add(warning("LONG_HORIZON", "Projection horizon exceeds 480 months."));
        }
        if (SimulationMath.defaulted(input.getApr()).compareTo(new BigDecimal("25")) >= 0) {
            warnings.add(warning("HIGH_APR", "APR is 25% or higher."));
        }
        if ((supports() == GoalType.RETIREMENT && input.getExpectedInflation() == null)
                || (supports() == GoalType.PURCHASE && input.getExpectedPriceInflation() == null)) {
            warnings.add(warning("INFLATION_NOT_SPECIFIED", "Inflation was not explicitly provided. Default inflation assumptions were used."));
        }
        if (supports() == GoalType.RETIREMENT) {
            BigDecimal withdrawalRate = input.getWithdrawalRate();
            if (withdrawalRate != null
                    && (withdrawalRate.compareTo(new BigDecimal("2")) < 0 || withdrawalRate.compareTo(new BigDecimal("8")) > 0)) {
                warnings.add(warning("WITHDRAWAL_RATE_OUT_OF_RANGE", "Withdrawal rate is outside the typical 2% to 8% range."));
            }
        }
        return warnings;
    }

    protected SimulationWarning warning(String code, String message) {
        return SimulationWarning.builder().code(code).message(message).build();
    }

    protected IncomeStrategy resolveIncomeStrategy(SimulationInput input) {
        return input.getIncomeStrategy() == null ? IncomeStrategy.COMPOUND : input.getIncomeStrategy();
    }
}
