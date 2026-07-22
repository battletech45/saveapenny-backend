package com.saveapenny.notification.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.notification.dto.CreateNotificationRequest;
import com.saveapenny.notification.dto.NotificationResponse;
import com.saveapenny.notification.dto.UnreadNotificationCountResponse;
import com.saveapenny.notification.dto.UpdateNotificationRequest;
import com.saveapenny.notification.entity.Notification;
import com.saveapenny.notification.entity.NotificationType;
import com.saveapenny.notification.exception.NotificationNotFoundException;
import com.saveapenny.notification.mapper.NotificationMapper;
import com.saveapenny.notification.repository.NotificationRepository;
import com.saveapenny.notification.service.NotificationService;
import com.saveapenny.push.service.PushNotificationSender;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final PushNotificationSender pushNotificationSender;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper,
            PushNotificationSender pushNotificationSender) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.pushNotificationSender = pushNotificationSender;
    }

    @Override
    public NotificationResponse create(UUID currentUserId, CreateNotificationRequest request) {
        Notification notification = notificationMapper.toEntity(request);
        notification.setUserId(currentUserId);
        Notification saved = notificationRepository.save(notification);
        pushNotificationSender.send(
                currentUserId, saved.getType(), saved.getTitle(), saved.getMessage(), pushData(saved.getType(), request.getMetadata()));
        return notificationMapper.toResponse(saved);
    }

    private Map<String, String> pushData(NotificationType type, JsonNode metadata) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", type.name());
        if (metadata != null && metadata.isObject()) {
            metadata.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isValueNode()) {
                    data.put(entry.getKey(), value.asText());
                }
            });
        }
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAll(UUID currentUserId, Boolean read, Pageable pageable) {
        Page<Notification> page = read == null
                ? notificationRepository.findAllByUserId(currentUserId, pageable)
                : notificationRepository.findAllByUserIdAndRead(currentUserId, read, pageable);
        return page.map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getById(UUID currentUserId, UUID notificationId) {
        Notification notification = findOwnedNotification(currentUserId, notificationId);
        return notificationMapper.toResponse(notification);
    }

    @Override
    public NotificationResponse update(UUID currentUserId, UUID notificationId, UpdateNotificationRequest request) {
        Notification notification = findOwnedNotification(currentUserId, notificationId);
        notification.setRead(request.getRead());
        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID currentUserId, UUID notificationId) {
        Notification notification = findOwnedNotification(currentUserId, notificationId);
        notificationRepository.delete(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(UUID currentUserId) {
        long unreadCount = notificationRepository.countByUserIdAndReadFalse(currentUserId);
        return UnreadNotificationCountResponse.builder()
                .unreadCount(unreadCount)
                .build();
    }

    @Override
    public void markAllAsRead(UUID currentUserId) {
        Page<Notification> unreadPage;
        do {
            unreadPage = notificationRepository.findAllByUserIdAndRead(currentUserId, false, PageRequest.of(0, 500));
            unreadPage.forEach(notification -> notification.setRead(true));
            if (!unreadPage.isEmpty()) {
                notificationRepository.saveAll(unreadPage.getContent());
            }
        } while (!unreadPage.isEmpty());
    }

    private Notification findOwnedNotification(UUID currentUserId, UUID notificationId) {
        return notificationRepository.findByIdAndUserId(notificationId, currentUserId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
    }
}
