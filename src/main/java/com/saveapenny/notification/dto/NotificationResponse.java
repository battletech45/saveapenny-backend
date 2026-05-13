package com.saveapenny.notification.dto;

import com.saveapenny.notification.entity.NotificationType;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String message;
    private Boolean read;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
