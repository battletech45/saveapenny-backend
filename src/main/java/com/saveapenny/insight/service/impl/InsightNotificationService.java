package com.saveapenny.insight.service.impl;

import com.saveapenny.insight.analytics.InsightCandidate;
import com.saveapenny.notification.dto.CreateNotificationRequest;
import com.saveapenny.notification.entity.NotificationType;
import com.saveapenny.notification.service.NotificationService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsightNotificationService {

    private final NotificationService notificationService;

    public InsightNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createInsightGeneratedNotification(UUID userId, InsightCandidate candidate) {
        notificationService.create(userId, CreateNotificationRequest.builder()
                .type(NotificationType.INSIGHT_GENERATED)
                .title(candidate.title())
                .message(candidate.summary())
                .build());
    }
}
