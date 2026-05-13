package com.saveapenny.notification.service.impl;

import com.saveapenny.notification.dto.CreateNotificationRequest;
import com.saveapenny.notification.dto.NotificationResponse;
import com.saveapenny.notification.dto.UnreadNotificationCountResponse;
import com.saveapenny.notification.dto.UpdateNotificationRequest;
import com.saveapenny.notification.entity.Notification;
import com.saveapenny.notification.exception.NotificationNotFoundException;
import com.saveapenny.notification.mapper.NotificationMapper;
import com.saveapenny.notification.repository.NotificationRepository;
import com.saveapenny.notification.service.NotificationService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    @Override
    public NotificationResponse create(UUID currentUserId, CreateNotificationRequest request) {
        Notification notification = notificationMapper.toEntity(request);
        notification.setUserId(currentUserId);
        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toResponse(saved);
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
        notificationRepository.findAllByUserIdAndRead(currentUserId, false, Pageable.unpaged())
                .forEach(notification -> notification.setRead(true));
    }

    private Notification findOwnedNotification(UUID currentUserId, UUID notificationId) {
        return notificationRepository.findByIdAndUserId(notificationId, currentUserId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
    }
}
