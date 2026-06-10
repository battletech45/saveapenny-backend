package com.saveapenny.notification.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.saveapenny.notification.dto.CreateNotificationRequest;
import com.saveapenny.notification.dto.NotificationResponse;
import com.saveapenny.notification.entity.Notification;
import com.saveapenny.notification.entity.NotificationType;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class NotificationMapperTest {

    private final NotificationMapper notificationMapper = Mappers.getMapper(NotificationMapper.class);

    @Test
    void toEntity_mapsCreateRequest() {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .type(NotificationType.BUDGET_WARNING)
                .title("Budget Alert")
                .message("You have exceeded your food budget.")
                .build();

        Notification entity = notificationMapper.toEntity(request);

        assertNull(entity.getId());
        assertNull(entity.getUserId());
        assertEquals(NotificationType.BUDGET_WARNING, entity.getType());
        assertEquals("Budget Alert", entity.getTitle());
        assertEquals("You have exceeded your food budget.", entity.getMessage());
        assertFalse(entity.getRead());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Notification entity = Notification.builder()
                .id(id)
                .userId(userId)
                .type(NotificationType.GOAL_OFF_TRACK)
                .title("Goal Update")
                .message("Your savings goal is off track.")
                .read(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        NotificationResponse response = notificationMapper.toResponse(entity);

        assertEquals(id, response.getId());
        assertEquals(userId, response.getUserId());
        assertEquals(NotificationType.GOAL_OFF_TRACK, response.getType());
        assertEquals("Goal Update", response.getTitle());
        assertEquals("Your savings goal is off track.", response.getMessage());
        assertEquals(true, response.getRead());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }
}
