package com.saveapenny.goal.simulation.strategy;

import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.simulation.MonthlyProjectionPoint;
import com.saveapenny.goal.simulation.SimulationInput;
import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.goal.simulation.math.SimulationMath;
import java.math.BigDecimal;
import java.time.LocalDate;

public class RetirementGoalStrategy extends AbstractGoalSimulationStrategy {

    @Override
    public GoalType supports() {
        return GoalType.RETIREMENT;
    }

    @Override
    public SimulationResult simulate(SimulationInput input) {
        LocalDate asOfDate = resolveAsOfDate(input);
        int yearsToRetirement = Math.max(SimulationMath.defaulted(input.getTargetRetirementAge(), 0) - SimulationMath.defaulted(input.getCurrentAge(), 0), 0);
        int months = yearsToRetirement * 12;
        BigDecimal currentSavings = SimulationMath.defaulted(input.getCurrentRetirementSavings());
        BigDecimal monthlyContribution = SimulationMath.defaulted(input.getMonthlyContribution());
        BigDecimal expectedAnnualReturn = input.getExpectedAnnualReturn() == null ? new BigDecimal("7") : input.getExpectedAnnualReturn();
        BigDecimal expectedInflation = input.getExpectedInflation() == null ? new BigDecimal("3") : input.getExpectedInflation();
        BigDecimal withdrawalRate = input.getWithdrawalRate() == null ? new BigDecimal("4") : input.getWithdrawalRate();
        BigDecimal desiredIncome = SimulationMath.defaulted(input.getDesiredMonthlyIncomeInRetirement());
        BigDecimal requiredNestEgg = desiredIncome.multiply(SimulationMath.TWELVE)
                .divide(SimulationMath.percentToRate(withdrawalRate), SimulationMath.MATH_CONTEXT);
        BigDecimal requiredFutureNestEgg = requiredNestEgg.multiply(
                SimulationMath.pow(BigDecimal.ONE.add(SimulationMath.percentToRate(expectedInflation)), yearsToRetirement),
                SimulationMath.MATH_CONTEXT);
        BigDecimal projectedNestEgg = SimulationMath.futureValue(currentSavings, monthlyContribution, expectedAnnualReturn, months);
        BigDecimal requiredContribution = SimulationMath.requiredContribution(requiredFutureNestEgg, currentSavings, expectedAnnualReturn, months);

        SimulationResult result = newResult(supports(), input, months);
        result.setFeasibility(classifyRetirement(requiredFutureNestEgg, projectedNestEgg));
        putCommonAssumptions(input, result, expectedAnnualReturn, currentSavings);
        result.getAssumptions().getValues().put("expectedInflation", SimulationMath.money(expectedInflation));
        result.getAssumptions().getValues().put("lifeExpectancy", SimulationMath.defaulted(input.getLifeExpectancy(), 85));
        result.getAssumptions().getValues().put("withdrawalRate", SimulationMath.money(withdrawalRate));
        result.getSummary().put("requiredNestEgg", SimulationMath.money(requiredFutureNestEgg));
        result.getSummary().put("projectedNestEgg", SimulationMath.money(projectedNestEgg));
        result.getSummary().put("shortfall", SimulationMath.money(requiredFutureNestEgg.subtract(projectedNestEgg)));
        result.getSummary().put("requiredMonthlyContribution", SimulationMath.money(requiredContribution));
        result.getSummary().put("currentMonthlyContribution", input.getMonthlyContribution() == null ? null : SimulationMath.money(monthlyContribution));

        BigDecimal balance = currentSavings;
        BigDecimal monthlyRate = SimulationMath.percentToMonthlyRate(expectedAnnualReturn);
        LocalDate month = asOfDate.withDayOfMonth(1).plusMonths(1);
        for (int i = 0; i < months; i++) {
            BigDecimal interest = balance.multiply(monthlyRate, SimulationMath.MATH_CONTEXT);
            balance = balance.add(interest).add(monthlyContribution);
            result.getSeries().add(MonthlyProjectionPoint.builder()
                    .month(month.plusMonths(i))
                    .balance(SimulationMath.money(balance))
                    .contribution(SimulationMath.money(monthlyContribution))
                    .interest(SimulationMath.money(interest))
                    .build());
        }
        return result;
    }

    private Feasibility classifyRetirement(BigDecimal requiredNestEgg, BigDecimal projectedNestEgg) {
        if (requiredNestEgg.compareTo(BigDecimal.ZERO) <= 0) {
            return Feasibility.ON_TRACK;
        }
        BigDecimal ratio = requiredNestEgg.subtract(projectedNestEgg)
                .divide(requiredNestEgg, SimulationMath.MATH_CONTEXT);
        if (ratio.compareTo(new BigDecimal("0.50")) > 0) {
            return Feasibility.INFEASIBLE;
        }
        if (ratio.compareTo(new BigDecimal("0.10")) >= 0) {
            return Feasibility.AT_RISK;
        }
        if (ratio.compareTo(BigDecimal.ZERO) >= 0) {
            return Feasibility.TIGHT;
        }
        return Feasibility.ON_TRACK;
    }
}
