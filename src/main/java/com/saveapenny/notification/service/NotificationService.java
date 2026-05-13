package com.saveapenny.notification.service;

import com.saveapenny.notification.dto.CreateNotificationRequest;
import com.saveapenny.notification.dto.NotificationResponse;
import com.saveapenny.notification.dto.UnreadNotificationCountResponse;
import com.saveapenny.notification.dto.UpdateNotificationRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationResponse create(UUID currentUserId, CreateNotificationRequest request);

    Page<NotificationResponse> getAll(UUID currentUserId, Boolean read, Pageable pageable);

    NotificationResponse getById(UUID currentUserId, UUID notificationId);

    NotificationResponse update(UUID currentUserId, UUID notificationId, UpdateNotificationRequest request);

    void delete(UUID currentUserId, UUID notificationId);

    UnreadNotificationCountResponse getUnreadCount(UUID currentUserId);

    void markAllAsRead(UUID currentUserId);
}
