package com.saveapenny.goal.simulation.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SimulationMathTest {

    @Test
    void monthsBetween_returnsPositive() {
        assertEquals(12, SimulationMath.monthsBetween(
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)));
    }

    @Test
    void monthsBetween_returnsZero_whenSameMonth() {
        assertEquals(0, SimulationMath.monthsBetween(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15)));
    }

    @Test
    void monthsBetween_returnsNegative_whenPastDate() {
        assertEquals(-12, SimulationMath.monthsBetween(
                LocalDate.of(2027, 1, 1), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void defaulted_returnsValue_whenNotNull() {
        assertEquals(new BigDecimal("100"), SimulationMath.defaulted(new BigDecimal("100")));
    }

    @Test
    void defaulted_returnsZero_whenNull() {
        assertEquals(BigDecimal.ZERO, SimulationMath.defaulted(null));
    }

    @Test
    void defaultedInt_returnsValue_whenNotNull() {
        assertEquals(5, SimulationMath.defaulted(Integer.valueOf(5), 10));
    }

    @Test
    void defaultedInt_returnsFallback_whenNull() {
        assertEquals(10, SimulationMath.defaulted(null, 10));
    }

    @Test
    void asStartOfDayUtc_converts() {
        LocalDate date = LocalDate.of(2026, 6, 10);
        OffsetDateTime result = SimulationMath.asStartOfDayUtc(date);
        assertEquals(OffsetDateTime.of(2026, 6, 10, 0, 0, 0, 0, ZoneOffset.UTC), result);
    }

    @Test
    void percentToMonthlyRate_converts() {
        BigDecimal result = SimulationMath.percentToMonthlyRate(new BigDecimal("12"));
        assertEquals(new BigDecimal("0.01"), result.stripTrailingZeros());
    }

    @Test
    void percentToMonthlyRate_zeroPercent() {
        assertEquals(BigDecimal.ZERO, SimulationMath.percentToMonthlyRate(BigDecimal.ZERO));
    }

    @Test
    void percentToRate_converts() {
        assertEquals(new BigDecimal("0.05"), SimulationMath.percentToRate(new BigDecimal("5")));
    }

    @Test
    void pow_withIntExponent() {
        assertEquals(0, SimulationMath.pow(new BigDecimal("2"), 3).compareTo(new BigDecimal("8")));
    }

    @Test
    void pow_withIntExponent_preservesDecimalPrecision() {
        BigDecimal result = SimulationMath.pow(new BigDecimal("1.01"), 12);
        assertEquals(0, result.compareTo(new BigDecimal("1.126825030131970")));
    }

    @Test
    void pow_withZeroExponent() {
        assertEquals(BigDecimal.ONE, SimulationMath.pow(new BigDecimal("100"), 0));
    }

    @Test
    void pow_withDoubleExponent() {
        BigDecimal result = SimulationMath.pow(new BigDecimal("4"), 0.5);
        assertEquals(0, new BigDecimal("2").compareTo(result.setScale(10, RoundingMode.HALF_UP)));
    }

    @Test
    void futureValue_noGrowth_whenRateIsZero() {
        BigDecimal result = SimulationMath.futureValue(
                new BigDecimal("1000"), new BigDecimal("100"), BigDecimal.ZERO, 12);
        assertEquals(0, result.compareTo(new BigDecimal("2200")));
    }

    @Test
    void futureValue_withGrowth() {
        BigDecimal result = SimulationMath.futureValue(
                new BigDecimal("1000"), new BigDecimal("100"), new BigDecimal("12"), 12);
        assertNotNull(result);
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void futureValue_returnsStart_whenMonthsZero() {
        BigDecimal result = SimulationMath.futureValue(
                new BigDecimal("500"), new BigDecimal("100"), new BigDecimal("10"), 0);
        assertEquals(new BigDecimal("500"), result);
    }

    @Test
    void futureValue_handlesNullStart() {
        BigDecimal result = SimulationMath.futureValue(
                null, new BigDecimal("100"), new BigDecimal("6"), 6);
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void requiredContribution_noGrowth_whenRateIsZero() {
        BigDecimal result = SimulationMath.requiredContribution(
                new BigDecimal("5000"), new BigDecimal("1000"), BigDecimal.ZERO, 12);
        assertEquals(0, result.compareTo(new BigDecimal("333.3333333333333")));
    }

    @Test
    void requiredContribution_returnsImmediateShortfall_whenMonthsZero() {
        BigDecimal result = SimulationMath.requiredContribution(
                new BigDecimal("5000"), new BigDecimal("1000"), new BigDecimal("10"), 0);
        assertEquals(new BigDecimal("4000"), result);
    }

    @Test
    void requiredContribution_returnsImmediateSurplus_whenMonthsPastAndAlreadyFunded() {
        BigDecimal result = SimulationMath.requiredContribution(
                new BigDecimal("5000"), new BigDecimal("6000"), new BigDecimal("10"), -3);
        assertEquals(new BigDecimal("-1000"), result);
    }

    @Test
    void safeDivide_returnsZero_whenDenominatorZero() {
        assertEquals(BigDecimal.ZERO, SimulationMath.safeDivide(new BigDecimal("100"), BigDecimal.ZERO));
    }

    @Test
    void safeDivide_returnsZero_whenDenominatorNull() {
        assertEquals(BigDecimal.ZERO, SimulationMath.safeDivide(new BigDecimal("100"), null));
    }

    @Test
    void safeDivide_normalDivision() {
        assertEquals(0, SimulationMath.safeDivide(new BigDecimal("10"), new BigDecimal("2")).compareTo(new BigDecimal("5")));
    }

    @Test
    void money_scalesToTwoDecimals() {
        assertEquals(new BigDecimal("123.46"), SimulationMath.money(new BigDecimal("123.4567")));
    }

    @Test
    void money_handlesNull() {
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), SimulationMath.money(null));
    }

    @Test
    void ratio_zeroWhenWholeIsZero() {
        assertEquals(BigDecimal.ZERO, SimulationMath.ratio(new BigDecimal("100"), BigDecimal.ZERO));
    }

    @Test
    void ratio_zeroWhenWholeIsNull() {
        assertEquals(BigDecimal.ZERO, SimulationMath.ratio(new BigDecimal("100"), null));
    }

    @Test
    void ratio_normal() {
        assertEquals(0, SimulationMath.ratio(new BigDecimal("50"), new BigDecimal("100")).compareTo(new BigDecimal("0.5")));
    }
}
