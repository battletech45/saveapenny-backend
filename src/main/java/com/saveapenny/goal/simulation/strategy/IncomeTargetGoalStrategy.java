package com.saveapenny.goal.simulation.strategy;

import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.simulation.IncomeStrategy;
import com.saveapenny.goal.simulation.MonthlyProjectionPoint;
import com.saveapenny.goal.simulation.SimulationInput;
import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.goal.simulation.math.SimulationMath;
import java.math.BigDecimal;
import java.time.LocalDate;

public class IncomeTargetGoalStrategy extends AbstractGoalSimulationStrategy {

    @Override
    public GoalType supports() {
        return GoalType.INCOME_TARGET;
    }

    @Override
    public SimulationResult simulate(SimulationInput input) {
        LocalDate asOfDate = resolveAsOfDate(input);
        int months = horizonMonths(asOfDate, input.getTargetDate());
        BigDecimal currentIncome = SimulationMath.defaulted(input.getCurrentAverageMonthlyNetIncome());
        BigDecimal targetIncome = SimulationMath.defaulted(input.getTargetMonthlyNetIncome());
        BigDecimal expectedGrowthRate = SimulationMath.defaulted(input.getExpectedIncomeGrowthRate());
        IncomeStrategy strategy = resolveIncomeStrategy(input);

        BigDecimal projectedIncome;
        BigDecimal requiredGrowth;
        BigDecimal currentGrowth;

        if (strategy == IncomeStrategy.LINEAR) {
            BigDecimal monthlyDelta = currentIncome.multiply(SimulationMath.percentToRate(expectedGrowthRate), SimulationMath.MATH_CONTEXT);
            projectedIncome = currentIncome.add(monthlyDelta.multiply(BigDecimal.valueOf(months), SimulationMath.MATH_CONTEXT));
            requiredGrowth = months == 0
                    ? BigDecimal.ZERO
                    : targetIncome.subtract(currentIncome).divide(BigDecimal.valueOf(months), SimulationMath.MATH_CONTEXT);
            currentGrowth = monthlyDelta;
        } else {
            projectedIncome = currentIncome;
            BigDecimal growthRate = SimulationMath.percentToRate(expectedGrowthRate);
            for (int i = 0; i < months; i++) {
                projectedIncome = projectedIncome.multiply(BigDecimal.ONE.add(growthRate), SimulationMath.MATH_CONTEXT);
            }
            if (currentIncome.compareTo(BigDecimal.ZERO) <= 0 || months == 0) {
                requiredGrowth = targetIncome.compareTo(currentIncome) > 0 ? new BigDecimal("999") : BigDecimal.ZERO;
            } else {
                requiredGrowth = SimulationMath.pow(
                                targetIncome.divide(currentIncome, SimulationMath.MATH_CONTEXT),
                                1.0 / months)
                        .subtract(BigDecimal.ONE)
                        .multiply(SimulationMath.HUNDRED);
            }
            currentGrowth = expectedGrowthRate;
        }

        SimulationResult result = newResult(supports(), input, months);
        result.setFeasibility(classify(requiredGrowth, strategy));
        putCommonAssumptions(input, result, BigDecimal.ZERO, BigDecimal.ZERO);
        result.getAssumptions().getValues().put("incomeStrategy", strategy.name());
        result.getAssumptions().getValues().put("expectedIncomeGrowthRate", SimulationMath.money(expectedGrowthRate));
        result.getSummary().put("targetMonthlyNetIncome", SimulationMath.money(targetIncome));
        result.getSummary().put("projectedMonthlyNetIncome", SimulationMath.money(projectedIncome));
        result.getSummary().put("requiredMonthlyGrowthRate", SimulationMath.money(requiredGrowth));
        result.getSummary().put("currentMonthlyGrowthRate", SimulationMath.money(currentGrowth));

        BigDecimal runningIncome = currentIncome;
        LocalDate month = asOfDate.withDayOfMonth(1).plusMonths(1);
        for (int i = 0; i < months; i++) {
            BigDecimal growth;
            if (strategy == IncomeStrategy.LINEAR) {
                growth = currentIncome.multiply(SimulationMath.percentToRate(expectedGrowthRate), SimulationMath.MATH_CONTEXT);
                runningIncome = runningIncome.add(growth);
            } else {
                growth = runningIncome.multiply(SimulationMath.percentToRate(expectedGrowthRate), SimulationMath.MATH_CONTEXT);
                runningIncome = runningIncome.add(growth);
            }
            result.getSeries().add(MonthlyProjectionPoint.builder()
                    .month(month.plusMonths(i))
                    .balance(SimulationMath.money(runningIncome))
                    .growth(SimulationMath.money(growth))
                    .build());
        }
        return result;
    }

    private Feasibility classify(BigDecimal requiredGrowth, IncomeStrategy strategy) {
        BigDecimal comparable = strategy == IncomeStrategy.LINEAR ? BigDecimal.ZERO : requiredGrowth;
        if (comparable.compareTo(new BigDecimal("5")) > 0) {
            return Feasibility.INFEASIBLE;
        }
        if (comparable.compareTo(new BigDecimal("2")) >= 0) {
            return Feasibility.AT_RISK;
        }
        if (comparable.compareTo(new BigDecimal("0.5")) >= 0) {
            return Feasibility.TIGHT;
        }
        return Feasibility.ON_TRACK;
    }
}
