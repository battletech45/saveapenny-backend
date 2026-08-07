package com.saveapenny.feedback.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.feedback.entity.Feedback;
import com.saveapenny.feedback.entity.FeedbackStatus;
import com.saveapenny.notification.dto.CreateNotificationRequest;
import com.saveapenny.notification.entity.NotificationType;
import com.saveapenny.notification.service.NotificationService;
import org.springframework.stereotype.Component;

@Component
public class FeedbackStatusNotifier {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public FeedbackStatusNotifier(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    public void notifyStatusChanged(Feedback feedback, FeedbackStatus previousStatus) {
        if (feedback.getStatus() == previousStatus) {
            return;
        }

        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .type(NotificationType.FEEDBACK_STATUS_UPDATED)
                .title("Your feedback was updated")
                .message("Your feedback is now " + feedback.getStatus().name() + ".")
                .metadata(objectMapper.createObjectNode().put("feedbackId", feedback.getId().toString()))
                .build();
        notificationService.create(feedback.getUserId(), request);
    }
}
