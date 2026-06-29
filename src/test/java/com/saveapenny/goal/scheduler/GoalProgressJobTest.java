package com.saveapenny.goal.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.goal.config.GoalProgressProperties;
import com.saveapenny.goal.entity.GoalEntity;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.notification.GoalOffTrackNotifier;
import com.saveapenny.goal.repository.GoalRepository;
import com.saveapenny.goal.service.GoalProgressCalculator;
import com.saveapenny.goal.service.GoalProgressReport;
import com.saveapenny.goal.service.GoalProgressReport.ProgressStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class GoalProgressJobTest {

    @Mock
    private GoalRepository goalRepository;
    @Mock
    private GoalProgressCalculator goalProgressCalculator;
    @Mock
    private GoalOffTrackNotifier goalOffTrackNotifier;

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Captor
    private ArgumentCaptor<GoalProgressReport> reportCaptor;

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2025-06-10T10:00:00Z"), ZoneId.of("UTC"));

    private GoalProgressJob job;
    private UUID goalId;
    private UUID userId;
    private GoalEntity activeGoal;

    private final GoalProgressProperties goalProgressProperties =
            new GoalProgressProperties(true, new BigDecimal("0.10"), new BigDecimal("0.05"), 2, "0 0 6 * * *");

    @BeforeEach
    void setUp() {
        job = new GoalProgressJob(goalProgressProperties, goalRepository, goalProgressCalculator, goalOffTrackNotifier, fixedClock, meterRegistry);

        goalId = UUID.randomUUID();
        userId = UUID.randomUUID();
        activeGoal = GoalEntity.builder()
                .id(goalId)
                .userId(userId)
                .status(GoalStatus.ACTIVE)
                .build();
    }

    @Test
    void evaluateAllActiveGoals_processesActiveGoals() {
        Page<GoalEntity> page = new PageImpl<>(List.of(activeGoal));
        when(goalRepository.findAllByStatusAndDeletedAtIsNull(GoalStatus.ACTIVE, PageRequest.of(0, 100)))
                .thenReturn(page);
        when(goalProgressCalculator.calculate(userId, goalId, LocalDate.now(fixedClock)))
                .thenReturn(newReport(ProgressStatus.ON_TRACK, 0));

        job.evaluateAllActiveGoals();

        verify(goalProgressCalculator).calculate(userId, goalId, LocalDate.now(fixedClock));
        verify(goalOffTrackNotifier).notifyIfTransitionedToOffTrack(userId, goalId, newReport(ProgressStatus.ON_TRACK, 0));
        assertEquals(1.0, meterRegistry.find("goal.progress.job.runs")
                .tags("outcome", "on_track")
                .counter()
                .count());
        assertEquals(1L, meterRegistry.find("goal.progress.job.duration")
                .timer()
                .count());
    }

    @Test
    void evaluateAllActiveGoals_skipsCalculationWhenNoActiveGoals() {
        Page<GoalEntity> emptyPage = new PageImpl<>(List.of());
        when(goalRepository.findAllByStatusAndDeletedAtIsNull(GoalStatus.ACTIVE, PageRequest.of(0, 100)))
                .thenReturn(emptyPage);

        job.evaluateAllActiveGoals();

        verify(goalProgressCalculator, never()).calculate(any(), any(), any());
        verify(goalOffTrackNotifier, never()).notifyIfTransitionedToOffTrack(any(), any(), any());
    }

    @Test
    void applyStreak_incrementsCountOnOffTrack() {
        Page<GoalEntity> page = new PageImpl<>(List.of(activeGoal));
        when(goalRepository.findAllByStatusAndDeletedAtIsNull(GoalStatus.ACTIVE, PageRequest.of(0, 100)))
                .thenReturn(page);
        when(goalProgressCalculator.calculate(userId, goalId, LocalDate.now(fixedClock)))
                .thenReturn(newReport(ProgressStatus.OFF_TRACK, 0));

        job.evaluateAllActiveGoals();
        job.evaluateAllActiveGoals();

        verify(goalOffTrackNotifier, times(2)).notifyIfTransitionedToOffTrack(
                eq(userId), eq(goalId), reportCaptor.capture());
        List<GoalProgressReport> capturedReports = reportCaptor.getAllValues();
        assertEquals(1, capturedReports.get(0).offTrackForMonthsCount());
        assertEquals(2, capturedReports.get(1).offTrackForMonthsCount());
    }

    @Test
    void applyStreak_resetsCountOnOnTrack() {
        Page<GoalEntity> page = new PageImpl<>(List.of(activeGoal));
        when(goalRepository.findAllByStatusAndDeletedAtIsNull(GoalStatus.ACTIVE, PageRequest.of(0, 100)))
                .thenReturn(page);
        when(goalProgressCalculator.calculate(userId, goalId, LocalDate.now(fixedClock)))
                .thenReturn(newReport(ProgressStatus.OFF_TRACK, 0))
                .thenReturn(newReport(ProgressStatus.ON_TRACK, 0));

        job.evaluateAllActiveGoals();
        job.evaluateAllActiveGoals();

        verify(goalOffTrackNotifier, times(2)).notifyIfTransitionedToOffTrack(
                eq(userId), eq(goalId), reportCaptor.capture());
        List<GoalProgressReport> capturedReports = reportCaptor.getAllValues();
        assertEquals(1, capturedReports.get(0).offTrackForMonthsCount());
        assertEquals(0, capturedReports.get(1).offTrackForMonthsCount());
    }

    @Test
    void evaluateAllActiveGoals_calculatorException_doesNotPropagate() {
        Page<GoalEntity> page = new PageImpl<>(List.of(activeGoal));
        when(goalRepository.findAllByStatusAndDeletedAtIsNull(GoalStatus.ACTIVE, PageRequest.of(0, 100)))
                .thenReturn(page);
        when(goalProgressCalculator.calculate(userId, goalId, LocalDate.now(fixedClock)))
                .thenThrow(new RuntimeException("calc error"));

        job.evaluateAllActiveGoals();

        verify(goalOffTrackNotifier, never()).notifyIfTransitionedToOffTrack(any(), any(), any());
        assertEquals(1.0, meterRegistry.find("goal.progress.job.runs")
                .tags("outcome", "error")
                .counter()
                .count());
        assertEquals(1.0, meterRegistry.find("goal.progress.job.failures")
                .counter()
                .count());
    }

    @Test
    void evaluateAllActiveGoals_notifierException_doesNotPropagate() {
        Page<GoalEntity> page = new PageImpl<>(List.of(activeGoal));
        when(goalRepository.findAllByStatusAndDeletedAtIsNull(GoalStatus.ACTIVE, PageRequest.of(0, 100)))
                .thenReturn(page);
        when(goalProgressCalculator.calculate(userId, goalId, LocalDate.now(fixedClock)))
                .thenReturn(newReport(ProgressStatus.ON_TRACK, 0));
        when(goalOffTrackNotifier.notifyIfTransitionedToOffTrack(userId, goalId, newReport(ProgressStatus.ON_TRACK, 0)))
                .thenThrow(new RuntimeException("notify error"));

        job.evaluateAllActiveGoals();
    }

    @Test
    void evaluateAllActiveGoals_paginatesThroughAllPages() {
        GoalEntity goal2 = GoalEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .status(GoalStatus.ACTIVE)
                .build();
        Page<GoalEntity> page1 = mockPage(List.of(activeGoal), true);
        Page<GoalEntity> page2 = mockPage(List.of(goal2), false);

        when(goalRepository.findAllByStatusAndDeletedAtIsNull(GoalStatus.ACTIVE, PageRequest.of(0, 100)))
                .thenReturn(page1);
        when(goalRepository.findAllByStatusAndDeletedAtIsNull(GoalStatus.ACTIVE, PageRequest.of(1, 100)))
                .thenReturn(page2);
        when(goalProgressCalculator.calculate(any(), any(), any()))
                .thenReturn(newReport(ProgressStatus.ON_TRACK, 0));

        job.evaluateAllActiveGoals();

        verify(goalProgressCalculator).calculate(userId, goalId, LocalDate.now(fixedClock));
        verify(goalProgressCalculator).calculate(goal2.getUserId(), goal2.getId(), LocalDate.now(fixedClock));
    }

    @SuppressWarnings("unchecked")
    private Page<GoalEntity> mockPage(List<GoalEntity> content, boolean hasNext) {
        Page<GoalEntity> page = mock(Page.class);
        when(page.getContent()).thenReturn(content);
        when(page.hasNext()).thenReturn(hasNext);
        return page;
    }

    private static GoalProgressReport newReport(ProgressStatus status, int offTrackCount) {
        return new GoalProgressReport(null, null, null, null, null, null, null, status, offTrackCount, List.of());
    }
}
