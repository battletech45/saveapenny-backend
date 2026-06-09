package com.saveapenny.goal.simulation.math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

public final class SimulationMath {

    public static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;
    public static final BigDecimal ZERO = BigDecimal.ZERO;
    public static final BigDecimal HUNDRED = new BigDecimal("100");
    public static final BigDecimal TWELVE = new BigDecimal("12");

    private SimulationMath() {}

    public static int monthsBetween(LocalDate asOfDate, LocalDate targetDate) {
        return (int) java.time.temporal.ChronoUnit.MONTHS.between(
                YearMonth.from(asOfDate),
                YearMonth.from(targetDate));
    }

    public static BigDecimal defaulted(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    public static int defaulted(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    public static OffsetDateTime asStartOfDayUtc(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    public static BigDecimal percentToMonthlyRate(BigDecimal annualPercent) {
        BigDecimal annualRate = defaulted(annualPercent).divide(HUNDRED, MATH_CONTEXT);
        return annualRate.divide(TWELVE, MATH_CONTEXT);
    }

    public static BigDecimal percentToRate(BigDecimal percent) {
        return defaulted(percent).divide(HUNDRED, MATH_CONTEXT);
    }

    public static BigDecimal pow(BigDecimal base, int exponent) {
        if (exponent == 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(Math.pow(base.doubleValue(), exponent));
    }

    public static BigDecimal pow(BigDecimal base, double exponent) {
        return BigDecimal.valueOf(Math.pow(base.doubleValue(), exponent));
    }

    public static BigDecimal futureValue(BigDecimal startBalance, BigDecimal contribution, BigDecimal annualPercent, int months) {
        BigDecimal start = defaulted(startBalance);
        BigDecimal monthly = defaulted(contribution);
        if (months <= 0) {
            return start;
        }

        BigDecimal rate = percentToMonthlyRate(annualPercent);
        if (rate.compareTo(ZERO) == 0) {
            return start.add(monthly.multiply(BigDecimal.valueOf(months)));
        }

        BigDecimal growth = pow(BigDecimal.ONE.add(rate), months);
        BigDecimal futureStart = start.multiply(growth, MATH_CONTEXT);
        BigDecimal annuityFactor = growth.subtract(BigDecimal.ONE).divide(rate, MATH_CONTEXT);
        return futureStart.add(monthly.multiply(annuityFactor, MATH_CONTEXT));
    }

    public static BigDecimal requiredContribution(BigDecimal targetAmount, BigDecimal startBalance, BigDecimal annualPercent, int months) {
        BigDecimal target = defaulted(targetAmount);
        BigDecimal start = defaulted(startBalance);
        if (months <= 0) {
            return ZERO;
        }

        BigDecimal rate = percentToMonthlyRate(annualPercent);
        if (rate.compareTo(ZERO) == 0) {
            return target.subtract(start).divide(BigDecimal.valueOf(months), MATH_CONTEXT);
        }

        BigDecimal growth = pow(BigDecimal.ONE.add(rate), months);
        BigDecimal numerator = target.subtract(start.multiply(growth, MATH_CONTEXT), MATH_CONTEXT)
                .multiply(rate, MATH_CONTEXT);
        BigDecimal denominator = growth.subtract(BigDecimal.ONE, MATH_CONTEXT);
        return numerator.divide(denominator, MATH_CONTEXT);
    }

    public static BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(ZERO) == 0) {
            return ZERO;
        }
        return defaulted(numerator).divide(denominator, MATH_CONTEXT);
    }

    public static BigDecimal money(BigDecimal value) {
        return defaulted(value).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal ratio(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.compareTo(ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return defaulted(part).divide(whole.abs(), MATH_CONTEXT);
    }
}
