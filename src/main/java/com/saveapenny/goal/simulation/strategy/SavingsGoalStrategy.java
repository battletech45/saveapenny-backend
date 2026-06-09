package com.saveapenny.goal.simulation.strategy;

import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.simulation.MonthlyProjectionPoint;
import com.saveapenny.goal.simulation.SimulationInput;
import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.goal.simulation.math.SimulationMath;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SavingsGoalStrategy extends AbstractGoalSimulationStrategy {

    @Override
    public GoalType supports() {
        return GoalType.SAVINGS;
    }

    @Override
    public SimulationResult simulate(SimulationInput input) {
        LocalDate asOfDate = resolveAsOfDate(input);
        int months = horizonMonths(asOfDate, input.getTargetDate());
        BigDecimal startBalance = SimulationMath.defaulted(input.getStartBalance());
        BigDecimal currentContribution = SimulationMath.defaulted(input.getMonthlyContribution());
        BigDecimal expectedAnnualReturn = SimulationMath.defaulted(input.getExpectedAnnualReturn());
        BigDecimal projectedAmount = SimulationMath.futureValue(startBalance, currentContribution, expectedAnnualReturn, months);
        BigDecimal requiredContribution = SimulationMath.requiredContribution(
                input.getTargetAmount(),
                startBalance,
                expectedAnnualReturn,
                months);

        SimulationResult result = newResult(supports(), input, months);
        result.setFeasibility(classifyByIncomeBands(
                requiredContribution.max(BigDecimal.ZERO),
                input.getAverageMonthlyNetIncome(),
                new BigDecimal("0.30"),
                new BigDecimal("0.50"),
                new BigDecimal("0.80")));
        putCommonAssumptions(input, result, expectedAnnualReturn, startBalance);
        result.getSummary().put("targetAmount", SimulationMath.money(input.getTargetAmount()));
        result.getSummary().put("projectedAmount", SimulationMath.money(projectedAmount));
        result.getSummary().put("shortfall", SimulationMath.money(SimulationMath.defaulted(input.getTargetAmount()).subtract(projectedAmount)));
        result.getSummary().put("requiredMonthlyContribution", SimulationMath.money(requiredContribution));
        result.getSummary().put("currentMonthlyContribution", input.getMonthlyContribution() == null ? null : SimulationMath.money(currentContribution));

        BigDecimal balance = startBalance;
        BigDecimal monthlyRate = SimulationMath.percentToMonthlyRate(expectedAnnualReturn);
        LocalDate month = asOfDate.withDayOfMonth(1).plusMonths(1);
        for (int i = 0; i < months; i++) {
            BigDecimal interest = balance.multiply(monthlyRate, SimulationMath.MATH_CONTEXT);
            balance = balance.add(interest).add(currentContribution);
            result.getSeries().add(MonthlyProjectionPoint.builder()
                    .month(month.plusMonths(i))
                    .balance(SimulationMath.money(balance))
                    .contribution(SimulationMath.money(currentContribution))
                    .interest(SimulationMath.money(interest))
                    .build());
        }
        return result;
    }
}
