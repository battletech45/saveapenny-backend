package com.saveapenny.goal.simulation.strategy;

import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.simulation.MonthlyProjectionPoint;
import com.saveapenny.goal.simulation.SimulationInput;
import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.goal.simulation.math.SimulationMath;
import java.math.BigDecimal;
import java.time.LocalDate;

public class DebtPayoffGoalStrategy extends AbstractGoalSimulationStrategy {

    private static final int MAX_MONTHS_WITHOUT_TARGET = 600;

    @Override
    public GoalType supports() {
        return GoalType.DEBT_PAYOFF;
    }

    @Override
    public SimulationResult simulate(SimulationInput input) {
        LocalDate asOfDate = resolveAsOfDate(input);
        BigDecimal balance = SimulationMath.defaulted(input.getCurrentBalance());
        BigDecimal apr = SimulationMath.defaulted(input.getApr());
        BigDecimal monthlyRate = SimulationMath.percentToMonthlyRate(apr);
        BigDecimal monthlyInterest = balance.multiply(monthlyRate, SimulationMath.MATH_CONTEXT);
        BigDecimal minimumPayment = input.getMinimumPayment() == null
                ? monthlyInterest.add(balance.multiply(new BigDecimal("0.01"), SimulationMath.MATH_CONTEXT))
                : input.getMinimumPayment();
        BigDecimal actualPayment = input.getFixedPayment() != null
                ? input.getFixedPayment()
                : (input.getMonthlyBudget() != null ? input.getMonthlyBudget() : minimumPayment);
        int months = input.getTargetPayoffDate() != null
                ? horizonMonths(asOfDate, input.getTargetPayoffDate())
                : MAX_MONTHS_WITHOUT_TARGET;
        BigDecimal requiredPayment = input.getTargetPayoffDate() != null
                ? requiredPayment(balance, monthlyRate, months)
                : actualPayment;

        SimulationResult result = newResult(supports(), input, months);
        putCommonAssumptions(input, result, BigDecimal.ZERO, BigDecimal.ZERO);
        result.getAssumptions().getValues().put("apr", SimulationMath.money(apr));
        result.getAssumptions().getValues().put("minimumPayment", SimulationMath.money(minimumPayment));
        result.getAssumptions().getValues().put("interestOnlyMonthlyInterest", SimulationMath.money(monthlyInterest));

        BigDecimal runningBalance = balance;
        LocalDate month = asOfDate.withDayOfMonth(1).plusMonths(1);
        int elapsedMonths = 0;
        for (int i = 0; i < months && runningBalance.compareTo(BigDecimal.ZERO) > 0; i++) {
            BigDecimal interest = runningBalance.multiply(monthlyRate, SimulationMath.MATH_CONTEXT);
            BigDecimal payment = actualPayment.min(runningBalance.add(interest));
            runningBalance = runningBalance.add(interest).subtract(payment);
            if (runningBalance.compareTo(BigDecimal.ZERO) < 0) {
                runningBalance = BigDecimal.ZERO;
            }
            result.getSeries().add(MonthlyProjectionPoint.builder()
                    .month(month.plusMonths(i))
                    .balance(SimulationMath.money(runningBalance))
                    .payment(SimulationMath.money(payment))
                    .interestCharged(SimulationMath.money(interest))
                    .build());
            elapsedMonths++;
        }
        result.setHorizonMonths(input.getTargetPayoffDate() != null ? months : elapsedMonths);
        result.getSummary().put("targetAmount", BigDecimal.ZERO.setScale(2));
        result.getSummary().put("projectedAmount", SimulationMath.money(runningBalance));
        result.getSummary().put("shortfall", SimulationMath.money(runningBalance));
        result.getSummary().put("requiredMonthlyContribution", SimulationMath.money(requiredPayment));
        result.getSummary().put("currentMonthlyContribution", SimulationMath.money(actualPayment));
        result.setFeasibility(classify(input, minimumPayment, actualPayment, monthlyInterest, requiredPayment, runningBalance));
        return result;
    }

    private BigDecimal requiredPayment(BigDecimal balance, BigDecimal monthlyRate, int months) {
        if (months <= 0) {
            return balance;
        }
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return balance.divide(BigDecimal.valueOf(months), SimulationMath.MATH_CONTEXT);
        }
        BigDecimal denominator = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(SimulationMath.pow(BigDecimal.ONE.add(monthlyRate), months), SimulationMath.MATH_CONTEXT),
                SimulationMath.MATH_CONTEXT);
        return balance.multiply(monthlyRate, SimulationMath.MATH_CONTEXT).divide(denominator, SimulationMath.MATH_CONTEXT);
    }

    private Feasibility classify(
            SimulationInput input,
            BigDecimal minimumPayment,
            BigDecimal actualPayment,
            BigDecimal monthlyInterest,
            BigDecimal requiredPayment,
            BigDecimal endingBalance) {
        if (minimumPayment.compareTo(monthlyInterest) < 0 || actualPayment.compareTo(monthlyInterest) < 0) {
            return Feasibility.INFEASIBLE;
        }
        if (input.getTargetPayoffDate() != null && endingBalance.compareTo(BigDecimal.ZERO) > 0) {
            return Feasibility.INFEASIBLE;
        }
        return classifyByIncomeBands(
                requiredPayment.max(BigDecimal.ZERO),
                input.getAverageMonthlyNetIncome(),
                new BigDecimal("0.30"),
                new BigDecimal("0.60"),
                new BigDecimal("999"));
    }
}
