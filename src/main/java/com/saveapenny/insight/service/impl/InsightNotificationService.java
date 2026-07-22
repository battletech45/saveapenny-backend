package com.saveapenny.insight.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public InsightNotificationService(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createInsightGeneratedNotification(UUID userId, UUID insightId, InsightCandidate candidate) {
        notificationService.create(userId, CreateNotificationRequest.builder()
                .type(NotificationType.INSIGHT_GENERATED)
                .title(candidate.title())
                .message(candidate.summary())
                .metadata(objectMapper.createObjectNode().put("insightId", insightId.toString()))
                .build());
    }
}
