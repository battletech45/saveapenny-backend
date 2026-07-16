package com.saveapenny.goal.notification;

import com.saveapenny.analytics.dto.AnalyticsEvent;
import com.saveapenny.analytics.service.AnalyticsEventPublisher;
import com.saveapenny.goal.config.GoalProgressProperties;
import com.saveapenny.goal.dto.GoalDetailResponse;
import com.saveapenny.goal.service.GoalProgressReport;
import com.saveapenny.goal.service.GoalService;
import com.saveapenny.notification.dto.CreateNotificationRequest;
import com.saveapenny.notification.dto.NotificationResponse;
import com.saveapenny.notification.entity.Notification;
import com.saveapenny.notification.entity.NotificationType;
import com.saveapenny.notification.repository.NotificationRepository;
import com.saveapenny.notification.service.NotificationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GoalOffTrackNotifier {

    private static final String TITLE_PREFIX = "Goal is off track: ";
    private static final String FOOTER = "This is informational, not a recommendation.";

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final GoalService goalService;
    private final GoalProgressProperties properties;
    private final AnalyticsEventPublisher analyticsEventPublisher;

    public GoalOffTrackNotifier(
            NotificationRepository notificationRepository,
            NotificationService notificationService,
            GoalService goalService,
            GoalProgressProperties properties,
            AnalyticsEventPublisher analyticsEventPublisher) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.goalService = goalService;
        this.properties = properties;
        this.analyticsEventPublisher = analyticsEventPublisher;
    }

    public Optional<NotificationResponse> notifyIfTransitionedToOffTrack(UUID userId, UUID goalId, GoalProgressReport report) {
        GoalDetailResponse goal = goalService.getById(userId, goalId);
        List<Notification> unread = notificationRepository.findAllByUserIdAndTypeAndReadFalse(userId, NotificationType.GOAL_OFF_TRACK);
        boolean existingUnread = unread.stream().anyMatch(item -> (TITLE_PREFIX + goal.getTitle()).equals(item.getTitle()));
        if (existingUnread) {
            return Optional.empty();
        }
        if (report.status() != GoalProgressReport.ProgressStatus.OFF_TRACK) {
            return Optional.empty();
        }
        if (report.offTrackForMonthsCount() < properties.offTrackPersistenceMonths()) {
            return Optional.empty();
        }

        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .type(NotificationType.GOAL_OFF_TRACK)
                .title(TITLE_PREFIX + goal.getTitle())
                .message(buildMessage(goal, report))
                .build();
        NotificationResponse notification = notificationService.create(userId, request);
        analyticsEventPublisher.publish(new AnalyticsEvent(
                "goal_off_track",
                Map.of("goal_id", goalId.toString(), "off_track_months", report.offTrackForMonthsCount())));
        return Optional.of(notification);
    }

    private String buildMessage(GoalDetailResponse goal, GoalProgressReport report) {
        BigDecimal ratio = BigDecimal.ZERO;
        if (goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) != 0 && report.gap() != null) {
            ratio = report.gap().divide(goal.getTargetAmount().abs(), java.math.MathContext.DECIMAL64)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return goal.getTitle()
                + ": target=" + formatMoney(goal.getTargetAmount())
                + ", projected=" + formatMoney(report.projectedAmountAtTarget())
                + ", current=" + formatMoney(report.currentAmount())
                + ". You are " + ratio + "% short of the target at the projected date after " + report.offTrackForMonthsCount() + " months. "
                + FOOTER;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        BigDecimal scaled = amount.stripTrailingZeros();
        if (scaled.scale() <= 0) {
            return scaled.toPlainString();
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
