package com.saveapenny.notification.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.notification.dto.CreateNotificationRequest;
import com.saveapenny.notification.dto.NotificationResponse;
import com.saveapenny.notification.dto.UpdateNotificationRequest;
import com.saveapenny.notification.dto.UnreadNotificationCountResponse;
import com.saveapenny.notification.entity.Notification;
import com.saveapenny.notification.entity.NotificationType;
import com.saveapenny.notification.exception.NotificationNotFoundException;
import com.saveapenny.notification.mapper.NotificationMapper;
import com.saveapenny.notification.repository.NotificationRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID userId;
    private UUID notificationId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        notification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .type(NotificationType.SYSTEM)
                .title("System")
                .message("Hello")
                .read(false)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void create_returnsResponse_whenValid() {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .type(NotificationType.SYSTEM)
                .title("System")
                .message("Hello")
                .build();
        Notification mapped = Notification.builder().type(NotificationType.SYSTEM).build();
        NotificationResponse response = NotificationResponse.builder().id(notificationId).build();

        when(notificationMapper.toEntity(request)).thenReturn(mapped);
        when(notificationRepository.save(mapped)).thenReturn(notification);
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        NotificationResponse result = notificationService.create(userId, request);

        assertEquals(notificationId, result.getId());
        assertEquals(userId, mapped.getUserId());
    }

    @Test
    void getById_throws_whenNotFound() {
        when(notificationRepository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.getById(userId, notificationId));
    }

    @Test
    void getAll_filtersByRead_whenProvided() {
        when(notificationRepository.findAllByUserIdAndRead(userId, false, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(java.util.List.of(notification)));
        when(notificationMapper.toResponse(notification)).thenReturn(NotificationResponse.builder().id(notificationId).build());

        var result = notificationService.getAll(userId, false, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findAllByUserIdAndRead(userId, false, PageRequest.of(0, 20));
    }

    @Test
    void update_setsReadFlag() {
        UpdateNotificationRequest request = UpdateNotificationRequest.builder().read(true).build();
        NotificationResponse response = NotificationResponse.builder().id(notificationId).read(true).build();

        when(notificationRepository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        NotificationResponse result = notificationService.update(userId, notificationId, request);

        assertEquals(true, notification.getRead());
        assertEquals(true, result.getRead());
    }

    @Test
    void getAll_withoutReadFilter_returnsAll() {
        when(notificationRepository.findAllByUserId(userId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(java.util.List.of(notification)));
        when(notificationMapper.toResponse(notification)).thenReturn(NotificationResponse.builder().id(notificationId).build());

        var result = notificationService.getAll(userId, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findAllByUserId(userId, PageRequest.of(0, 20));
    }

    @Test
    void markAllAsRead_setsAllUnreadToTrue() {
        Notification unread1 = Notification.builder().id(UUID.randomUUID()).userId(userId).read(false).build();
        Notification unread2 = Notification.builder().id(UUID.randomUUID()).userId(userId).read(false).build();
        when(notificationRepository.findAllByUserIdAndRead(userId, false, org.springframework.data.domain.Pageable.unpaged()))
                .thenReturn(new PageImpl<>(java.util.List.of(unread1, unread2)));

        notificationService.markAllAsRead(userId);

        assertEquals(true, unread1.getRead());
        assertEquals(true, unread2.getRead());
    }

    @Test
    void delete_throws_whenMissing() {
        when(notificationRepository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.delete(userId, notificationId));
        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(5L);

        UnreadNotificationCountResponse result = notificationService.getUnreadCount(userId);

        assertEquals(5L, result.getUnreadCount());
    }
}
