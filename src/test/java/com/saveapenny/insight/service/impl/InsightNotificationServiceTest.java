package com.saveapenny.insight.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.insight.analytics.InsightCandidate;
import com.saveapenny.insight.entity.InsightType;
import com.saveapenny.notification.dto.CreateNotificationRequest;
import com.saveapenny.notification.dto.NotificationResponse;
import com.saveapenny.notification.entity.NotificationType;
import com.saveapenny.notification.service.NotificationService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsightNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<CreateNotificationRequest> requestCaptor;

    @Test
    void createInsightGeneratedNotification_attachesInsightIdToMetadata() {
        InsightNotificationService service = new InsightNotificationService(notificationService, new ObjectMapper());
        UUID userId = UUID.randomUUID();
        UUID insightId = UUID.randomUUID();
        InsightCandidate candidate = new InsightCandidate(
                InsightType.SPENDING_PATTERN, "Spike", "Summary", "Detail", null, "WARNING", null);
        when(notificationService.create(any(), any())).thenReturn(NotificationResponse.builder().id(UUID.randomUUID()).build());

        service.createInsightGeneratedNotification(userId, insightId, candidate);

        verify(notificationService).create(org.mockito.ArgumentMatchers.eq(userId), requestCaptor.capture());
        CreateNotificationRequest request = requestCaptor.getValue();
        assertEquals(NotificationType.INSIGHT_GENERATED, request.getType());
        assertEquals("Spike", request.getTitle());
        assertEquals("Summary", request.getMessage());
        assertEquals(insightId.toString(), request.getMetadata().get("insightId").asText());
    }
}
