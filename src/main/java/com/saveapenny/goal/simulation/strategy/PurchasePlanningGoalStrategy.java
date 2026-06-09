package com.saveapenny.goal.simulation.strategy;

import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.simulation.MonthlyProjectionPoint;
import com.saveapenny.goal.simulation.SimulationInput;
import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.goal.simulation.math.SimulationMath;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PurchasePlanningGoalStrategy extends AbstractGoalSimulationStrategy {

    @Override
    public GoalType supports() {
        return GoalType.PURCHASE;
    }

    @Override
    public SimulationResult simulate(SimulationInput input) {
        LocalDate asOfDate = resolveAsOfDate(input);
        int months = horizonMonths(asOfDate, input.getTargetDate());
        BigDecimal annualInflation = SimulationMath.defaulted(input.getExpectedPriceInflation());
        double years = months / 12.0;
        BigDecimal inflatedPrice = SimulationMath.defaulted(input.getTargetPrice())
                .multiply(SimulationMath.pow(BigDecimal.ONE.add(SimulationMath.percentToRate(annualInflation)), years), SimulationMath.MATH_CONTEXT);
        BigDecimal downPaymentPercent = input.getDownPaymentPercent() == null ? new BigDecimal("20") : input.getDownPaymentPercent();
        BigDecimal requiredDownPayment = inflatedPrice.multiply(downPaymentPercent, SimulationMath.MATH_CONTEXT)
                .divide(SimulationMath.HUNDRED, SimulationMath.MATH_CONTEXT);
        BigDecimal currentDownPayment = SimulationMath.defaulted(input.getCurrentDownPayment());
        BigDecimal monthlySaving = SimulationMath.defaulted(input.getMonthlySaving());
        BigDecimal expectedAnnualReturn = SimulationMath.defaulted(input.getExpectedAnnualReturn());
        BigDecimal projectedAmount = SimulationMath.futureValue(currentDownPayment, monthlySaving, expectedAnnualReturn, months);
        BigDecimal requiredContribution = SimulationMath.requiredContribution(requiredDownPayment, currentDownPayment, expectedAnnualReturn, months);

        SimulationResult result = newResult(supports(), input, months);
        result.setFeasibility(classifyByIncomeBands(
                requiredContribution.max(BigDecimal.ZERO),
                input.getAverageMonthlyNetIncome(),
                new BigDecimal("0.30"),
                new BigDecimal("0.50"),
                new BigDecimal("0.80")));
        putCommonAssumptions(input, result, expectedAnnualReturn, currentDownPayment);
        result.getAssumptions().getValues().put("expectedPriceInflation", SimulationMath.money(annualInflation));
        result.getSummary().put("targetAmount", SimulationMath.money(requiredDownPayment));
        result.getSummary().put("projectedAmount", SimulationMath.money(projectedAmount));
        result.getSummary().put("shortfall", SimulationMath.money(requiredDownPayment.subtract(projectedAmount)));
        result.getSummary().put("requiredMonthlyContribution", SimulationMath.money(requiredContribution));
        result.getSummary().put("currentMonthlyContribution", input.getMonthlySaving() == null ? null : SimulationMath.money(monthlySaving));

        BigDecimal balance = currentDownPayment;
        BigDecimal monthlyRate = SimulationMath.percentToMonthlyRate(expectedAnnualReturn);
        LocalDate month = asOfDate.withDayOfMonth(1).plusMonths(1);
        for (int i = 0; i < months; i++) {
            BigDecimal interest = balance.multiply(monthlyRate, SimulationMath.MATH_CONTEXT);
            balance = balance.add(interest).add(monthlySaving);
            result.getSeries().add(MonthlyProjectionPoint.builder()
                    .month(month.plusMonths(i))
                    .balance(SimulationMath.money(balance))
                    .contribution(SimulationMath.money(monthlySaving))
                    .interest(SimulationMath.money(interest))
                    .build());
        }
        return result;
    }
}
