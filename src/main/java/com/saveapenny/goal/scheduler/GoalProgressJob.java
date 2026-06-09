package com.saveapenny.goal.scheduler;

import com.saveapenny.goal.entity.GoalEntity;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.notification.GoalOffTrackNotifier;
import com.saveapenny.goal.repository.GoalRepository;
import com.saveapenny.goal.service.GoalProgressCalculator;
import com.saveapenny.goal.service.GoalProgressReport;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
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

    private final GoalRepository goalRepository;
    private final GoalProgressCalculator goalProgressCalculator;
    private final GoalOffTrackNotifier goalOffTrackNotifier;
    private final Clock assistantClock;
    private final Map<UUID, Integer> offTrackStreaks = new ConcurrentHashMap<>();

    public GoalProgressJob(
            GoalRepository goalRepository,
            GoalProgressCalculator goalProgressCalculator,
            GoalOffTrackNotifier goalOffTrackNotifier,
            Clock assistantClock) {
        this.goalRepository = goalRepository;
        this.goalProgressCalculator = goalProgressCalculator;
        this.goalOffTrackNotifier = goalOffTrackNotifier;
        this.assistantClock = assistantClock;
    }

    @Scheduled(cron = "${goal.progress.cron:0 0 6 * * *}")
    public void evaluateAllActiveGoals() {
        LocalDate asOf = LocalDate.now(assistantClock);
        int pageNumber = 0;
        Page<GoalEntity> page;
        do {
            page = goalRepository.findAllByStatusAndDeletedAtIsNull(GoalStatus.ACTIVE, PageRequest.of(pageNumber, 100));
            for (GoalEntity goal : page.getContent()) {
                try {
                    GoalProgressReport report = goalProgressCalculator.calculate(goal.getUserId(), goal.getId(), asOf);
                    report = applyStreak(goal.getId(), report);
                    goalOffTrackNotifier.notifyIfTransitionedToOffTrack(goal.getUserId(), goal.getId(), report);
                } catch (RuntimeException ex) {
                    log.warn("Failed to evaluate goal progress for {}", goal.getId(), ex);
                }
            }
            pageNumber++;
        } while (page.hasNext());
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
