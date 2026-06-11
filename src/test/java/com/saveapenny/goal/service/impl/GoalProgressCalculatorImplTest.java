package com.saveapenny.goal.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.goal.config.GoalProgressProperties;
import com.saveapenny.goal.dto.GoalDetailResponse;
import com.saveapenny.goal.dto.GoalRunResponse;
import com.saveapenny.goal.dto.ScenarioResponse;
import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalRunTrigger;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.service.GoalProgressReport;
import com.saveapenny.goal.service.GoalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalProgressCalculatorImplTest {

    @Mock
    private GoalService goalService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void calculate_returnsNoProjection_whenNoBaselineRunExists() throws Exception {
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(goalId)))
                .thenReturn(goalDetail(goalId, null, LocalDate.now().plusMonths(6), new BigDecimal("1000.00")));

        GoalProgressReport report = new GoalProgressCalculatorImpl(goalService, properties())
                .calculate(UUID.randomUUID(), goalId, LocalDate.now());

        assertEquals(GoalProgressReport.ProgressStatus.NO_PROJECTION, report.status());
        assertEquals("NO_PROJECTION", report.warnings().getFirst().code());
    }

    @Test
    void calculate_returnsAchieved_whenCurrentAmountMeetsTarget() throws Exception {
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(goalId)))
                .thenReturn(goalDetail(goalId, null, LocalDate.now().plusMonths(6), new BigDecimal("25000.00")));

        GoalProgressReport report = new GoalProgressCalculatorImpl(goalService, properties())
                .calculate(UUID.randomUUID(), goalId, LocalDate.now());

        assertEquals(GoalProgressReport.ProgressStatus.ACHIEVED, report.status());
    }

    @Test
    void calculate_returnsOnTrack_whenProjectedAmountIsAtOrBelowCurrentAmount() throws Exception {
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(goalId)))
                .thenReturn(goalDetail(goalId, runWithProjectedAmount(new BigDecimal("21000.00")), LocalDate.now().plusMonths(6), new BigDecimal("1000.00")));

        GoalProgressReport report = new GoalProgressCalculatorImpl(goalService, properties())
                .calculate(UUID.randomUUID(), goalId, LocalDate.now());

        assertEquals(GoalProgressReport.ProgressStatus.ON_TRACK, report.status());
        assertEquals(0, report.gap().compareTo(new BigDecimal("-1000.00")));
    }

    @Test
    void calculate_returnsAtRisk_whenDeficitRatioFallsInMiddleBand() throws Exception {
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(goalId)))
                .thenReturn(goalDetail(goalId, runWithProjectedAmount(new BigDecimal("18600.00")), LocalDate.now().plusMonths(6), new BigDecimal("1000.00")));

        GoalProgressReport report = new GoalProgressCalculatorImpl(goalService, properties())
                .calculate(UUID.randomUUID(), goalId, LocalDate.now());

        assertEquals(GoalProgressReport.ProgressStatus.AT_RISK, report.status());
        assertEquals(0, report.gap().compareTo(new BigDecimal("1400.00")));
    }

    @Test
    void calculate_returnsOffTrack_whenDeficitRatioExceedsThreshold() throws Exception {
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(goalId)))
                .thenReturn(goalDetail(goalId, runWithProjectedAmount(new BigDecimal("17000.00")), LocalDate.now().plusMonths(6), new BigDecimal("1000.00")));

        GoalProgressReport report = new GoalProgressCalculatorImpl(goalService, properties())
                .calculate(UUID.randomUUID(), goalId, LocalDate.now());

        assertEquals(GoalProgressReport.ProgressStatus.OFF_TRACK, report.status());
        assertEquals(1, report.offTrackForMonthsCount());
        assertEquals(0, report.gap().compareTo(new BigDecimal("3000.00")));
    }

    @Test
    void calculate_returnsOffTrack_whenProjectionDoesNotGrowButTargetIsFarAway() throws Exception {
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(goalId)))
                .thenReturn(goalDetail(goalId, runWithProjectedAmount(new BigDecimal("100.00")), LocalDate.now().plusMonths(6), new BigDecimal("100.00")));

        GoalProgressReport report = new GoalProgressCalculatorImpl(goalService, properties())
                .calculate(UUID.randomUUID(), goalId, LocalDate.now());

        assertEquals(GoalProgressReport.ProgressStatus.OFF_TRACK, report.status());
        assertEquals(0, report.gap().compareTo(new BigDecimal("19900.00")));
    }

    @Test
    void calculate_returnsTargetDatePassedWarning_whenTargetDateExpired() throws Exception {
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(goalId)))
                .thenReturn(goalDetail(goalId, null, LocalDate.now().minusDays(1), new BigDecimal("100.00")));

        GoalProgressReport report = new GoalProgressCalculatorImpl(goalService, properties())
                .calculate(UUID.randomUUID(), goalId, LocalDate.now());

        assertEquals(GoalProgressReport.ProgressStatus.OFF_TRACK, report.status());
        assertEquals("TARGET_DATE_PASSED", report.warnings().getFirst().code());
    }

    @Test
    void calculate_addsMissingCurrentBalanceWarning_whenNoBalanceFieldExists() throws Exception {
        UUID goalId = UUID.randomUUID();
        GoalDetailResponse goal = GoalDetailResponse.builder()
                .id(goalId)
                .type(GoalType.SAVINGS)
                .title("Goal")
                .status(GoalStatus.ACTIVE)
                .targetAmount(new BigDecimal("20000.00"))
                .currency("USD")
                .targetDate(LocalDate.now().plusMonths(6))
                .inputs(objectMapper.readTree("{\"version\":1,\"type\":\"SAVINGS\",\"values\":{}}"))
                .scenarios(List.of(ScenarioResponse.builder().id(UUID.randomUUID()).name("Baseline").isBaseline(true).build()))
                .build();
        when(goalService.getById(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(goalId))).thenReturn(goal);

        GoalProgressReport report = new GoalProgressCalculatorImpl(goalService, properties())
                .calculate(UUID.randomUUID(), goalId, LocalDate.now());

        assertEquals("CURRENT_BALANCE_MISSING", report.warnings().getFirst().code());
    }

    private GoalDetailResponse goalDetail(UUID goalId, GoalRunResponse latestRun, LocalDate targetDate, BigDecimal currentAmount) throws Exception {
        UUID baselineScenarioId = latestRun == null ? UUID.randomUUID() : latestRun.getScenarioId();
        return GoalDetailResponse.builder()
                .id(goalId)
                .type(GoalType.SAVINGS)
                .title("House Fund")
                .status(GoalStatus.ACTIVE)
                .targetAmount(new BigDecimal("20000.00"))
                .currency("USD")
                .targetDate(targetDate)
                .inputs(objectMapper.readTree("{\"version\":1,\"type\":\"SAVINGS\",\"values\":{\"startBalance\":" + currentAmount + "}}"))
                .scenarios(List.of(ScenarioResponse.builder().id(baselineScenarioId).name("Baseline").isBaseline(true).build()))
                .latestRun(latestRun)
                .build();
    }

    private GoalRunResponse runWithProjectedAmount(BigDecimal projectedAmount) throws Exception {
        UUID scenarioId = UUID.randomUUID();
        return GoalRunResponse.builder()
                .id(UUID.randomUUID())
                .scenarioId(scenarioId)
                .feasibility(Feasibility.ON_TRACK)
                .triggeredBy(GoalRunTrigger.USER)
                .createdAt(OffsetDateTime.now())
                .outputSummary(objectMapper.readTree("{\"projectedAmount\":" + projectedAmount + "}"))
                .build();
    }

    private GoalProgressProperties properties() {
        return new GoalProgressProperties(true, new BigDecimal("0.10"), new BigDecimal("0.05"), 2, "0 0 6 * * *");
    }
}
