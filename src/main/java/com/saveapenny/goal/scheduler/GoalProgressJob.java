package com.saveapenny.goal.scheduler;

import com.saveapenny.goal.entity.GoalEntity;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.notification.GoalOffTrackNotifier;
import com.saveapenny.goal.config.GoalProgressProperties;
import com.saveapenny.goal.repository.GoalRepository;
import com.saveapenny.goal.service.GoalProgressCalculator;
import com.saveapenny.goal.service.GoalProgressReport;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GoalProgressJob {

    private static final Logger log = LoggerFactory.getLogger(GoalProgressJob.class);

    private final GoalProgressProperties goalProgressProperties;
    private final GoalRepository goalRepository;
    private final GoalProgressCalculator goalProgressCalculator;
    private final GoalOffTrackNotifier goalOffTrackNotifier;
    private final Clock assistantClock;
    private final MeterRegistry meterRegistry;
    private final Map<UUID, Integer> offTrackStreaks = new ConcurrentHashMap<>();

    public GoalProgressJob(
            GoalProgressProperties goalProgressProperties,
            GoalRepository goalRepository,
            GoalProgressCalculator goalProgressCalculator,
            GoalOffTrackNotifier goalOffTrackNotifier,
            Clock assistantClock,
            MeterRegistry meterRegistry) {
        this.goalProgressProperties = goalProgressProperties;
        this.goalRepository = goalRepository;
        this.goalProgressCalculator = goalProgressCalculator;
        this.goalOffTrackNotifier = goalOffTrackNotifier;
        this.assistantClock = assistantClock;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(cron = "${goal.progress.cron:0 0 6 * * *}")
    public void evaluateAllActiveGoals() {
        if (!goalProgressProperties.enabled()) {
            return;
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        LocalDate asOf = LocalDate.now(assistantClock);
        Set<UUID> processedGoalIds = new HashSet<>();
        int pageNumber = 0;
        int failed = 0;
        int processed = 0;
        Page<GoalEntity> page;
        log.info("goal_progress_job_started asOf={}", asOf);
        do {
            page = goalRepository.findAllByStatusAndDeletedAtIsNull(GoalStatus.ACTIVE, PageRequest.of(pageNumber, 100));
            for (GoalEntity goal : page.getContent()) {
                processedGoalIds.add(goal.getId());
                try {
                    GoalProgressReport report = goalProgressCalculator.calculate(goal.getUserId(), goal.getId(), asOf);
                    report = applyStreak(goal.getId(), report);
                    goalOffTrackNotifier.notifyIfTransitionedToOffTrack(goal.getUserId(), goal.getId(), report);
                    processed++;
                    meterRegistry.counter("goal.progress.job.runs", "outcome", toOutcomeTag(report.status())).increment();
                } catch (RuntimeException ex) {
                    meterRegistry.counter("goal.progress.job.runs", "outcome", "error").increment();
                    meterRegistry.counter("goal.progress.job.failures").increment();
                    log.warn("goal_progress_job_failed goalId={} result=error", goal.getId(), ex);
                    failed++;
                }
            }
            pageNumber++;
        } while (page.hasNext());
        offTrackStreaks.keySet().removeIf(id -> !processedGoalIds.contains(id));
        sample.stop(meterRegistry.timer("goal.progress.job.duration"));
        log.info("goal_progress_job_completed processed={} failures={} asOf={}", processed, failed, asOf);
    }

    private String toOutcomeTag(GoalProgressReport.ProgressStatus status) {
        return status.name().toLowerCase();
    }

    private GoalProgressReport applyStreak(UUID goalId, GoalProgressReport report) {
        if (report.status() == GoalProgressReport.ProgressStatus.OFF_TRACK) {
            int streak = offTrackStreaks.merge(goalId, 1, Integer::sum);
            return report.withOffTrackForMonthsCount(streak);
        }
        offTrackStreaks.remove(goalId);
        return report.withOffTrackForMonthsCount(0);
    }
}
