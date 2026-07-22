package com.saveapenny.goal.notification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalOffTrackNotifierTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private GoalService goalService;
    @Mock
    private AnalyticsEventPublisher analyticsEventPublisher;

    @org.mockito.Captor
    private ArgumentCaptor<CreateNotificationRequest> requestCaptor;

    @Test
    void notifyIfTransitionedToOffTrack_skipsWhenUnreadNotificationExists() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(userId, goalId)).thenReturn(goal(goalId));
        when(notificationRepository.findAllByUserIdAndTypeAndReadFalse(userId, NotificationType.GOAL_OFF_TRACK))
                .thenReturn(List.of(Notification.builder().title("Goal is off track: House Fund").build()));

        Optional<NotificationResponse> response = notifier().notifyIfTransitionedToOffTrack(userId, goalId, offTrackReport(goalId, 2));

        assertFalse(response.isPresent());
        verify(notificationService, never()).create(any(), any());
    }

    @Test
    void notifyIfTransitionedToOffTrack_createsNotificationWhenThresholdMet() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(userId, goalId)).thenReturn(goal(goalId));
        when(notificationRepository.findAllByUserIdAndTypeAndReadFalse(userId, NotificationType.GOAL_OFF_TRACK)).thenReturn(List.of());
        when(notificationService.create(any(), any())).thenReturn(NotificationResponse.builder().id(UUID.randomUUID()).build());

        Optional<NotificationResponse> response = notifier().notifyIfTransitionedToOffTrack(userId, goalId, offTrackReport(goalId, 2));

        assertTrue(response.isPresent());
        verify(notificationService).create(any(), any());
    }

    @Test
    void notifyIfTransitionedToOffTrack_buildsTargetBasedMessage() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(userId, goalId)).thenReturn(goal(goalId));
        when(notificationRepository.findAllByUserIdAndTypeAndReadFalse(userId, NotificationType.GOAL_OFF_TRACK)).thenReturn(List.of());
        when(notificationService.create(any(), any())).thenReturn(NotificationResponse.builder().id(UUID.randomUUID()).build());

        notifier().notifyIfTransitionedToOffTrack(userId, goalId, offTrackReport(goalId, 2));

        verify(notificationService).create(any(), requestCaptor.capture());
        assertEquals(
                "House Fund: target=20000, projected=17000, current=1000. You are 15.00% short of the target at the projected date after 2 months. This is informational, not a recommendation.",
                requestCaptor.getValue().getMessage());
    }

    @Test
    void notifyIfTransitionedToOffTrack_attachesGoalIdToMetadata() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(userId, goalId)).thenReturn(goal(goalId));
        when(notificationRepository.findAllByUserIdAndTypeAndReadFalse(userId, NotificationType.GOAL_OFF_TRACK)).thenReturn(List.of());
        when(notificationService.create(any(), any())).thenReturn(NotificationResponse.builder().id(UUID.randomUUID()).build());

        notifier().notifyIfTransitionedToOffTrack(userId, goalId, offTrackReport(goalId, 2));

        verify(notificationService).create(any(), requestCaptor.capture());
        assertEquals(goalId.toString(), requestCaptor.getValue().getMetadata().get("goalId").asText());
    }

    @Test
    void notifyIfTransitionedToOffTrack_skipsWhenStillBelowThreshold() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(userId, goalId)).thenReturn(goal(goalId));
        when(notificationRepository.findAllByUserIdAndTypeAndReadFalse(userId, NotificationType.GOAL_OFF_TRACK)).thenReturn(List.of());

        Optional<NotificationResponse> response = notifier().notifyIfTransitionedToOffTrack(userId, goalId, offTrackReport(goalId, 1));

        assertFalse(response.isPresent());
        verify(notificationService, never()).create(any(), any());
    }

    @Test
    void notifyIfTransitionedToOffTrack_skipsWhenRecovered() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(userId, goalId)).thenReturn(goal(goalId));
        when(notificationRepository.findAllByUserIdAndTypeAndReadFalse(userId, NotificationType.GOAL_OFF_TRACK)).thenReturn(List.of());

        Optional<NotificationResponse> response = notifier().notifyIfTransitionedToOffTrack(
                userId,
                goalId,
                new GoalProgressReport(goalId, UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, 1,
                        GoalProgressReport.ProgressStatus.ACHIEVED, 0, List.of()));

        assertFalse(response.isPresent());
    }

    private GoalOffTrackNotifier notifier() {
        return new GoalOffTrackNotifier(
                notificationRepository,
                notificationService,
                goalService,
                new GoalProgressProperties(true, new BigDecimal("0.10"), new BigDecimal("0.05"), 2, "0 0 6 * * *"),
                analyticsEventPublisher,
                new ObjectMapper());
    }

    private GoalDetailResponse goal(UUID goalId) {
        return GoalDetailResponse.builder()
                .id(goalId)
                .title("House Fund")
                .targetAmount(new BigDecimal("20000.00"))
                .targetDate(LocalDate.now().plusMonths(6))
                .build();
    }

    private GoalProgressReport offTrackReport(UUID goalId, int streak) {
        return new GoalProgressReport(
                goalId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("1000.00"),
                new BigDecimal("17000.00"),
                new BigDecimal("3000.00"),
                6,
                GoalProgressReport.ProgressStatus.OFF_TRACK,
                streak,
                List.of());
    }
}
