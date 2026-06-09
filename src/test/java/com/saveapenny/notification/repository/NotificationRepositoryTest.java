package com.saveapenny.notification.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.notification.entity.Notification;
import com.saveapenny.notification.entity.NotificationType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private UUID userId;
    private Notification readNotification;
    private Notification unreadNotification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        readNotification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.SYSTEM)
                .title("Read Alert")
                .message("This is a read notification.")
                .read(true)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();

        unreadNotification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.SYSTEM)
                .title("Unread Alert")
                .message("This is an unread notification.")
                .read(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        notificationRepository.save(readNotification);
        notificationRepository.save(unreadNotification);
    }

    @Test
    void findByIdAndUserId_returnsNotification() {
        Optional<Notification> found = notificationRepository.findByIdAndUserId(
                readNotification.getId(), userId);
        assertTrue(found.isPresent());
        assertEquals("Read Alert", found.get().getTitle());
    }

    @Test
    void findByIdAndUserId_returnsEmptyForWrongUser() {
        Optional<Notification> found = notificationRepository.findByIdAndUserId(
                readNotification.getId(), UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    @Test
    void findAllByUserId_returnsAll() {
        Page<Notification> page = notificationRepository.findAllByUserId(userId, PageRequest.of(0, 20));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndRead_filtersByReadState() {
        Page<Notification> unreadPage = notificationRepository.findAllByUserIdAndRead(
                userId, false, PageRequest.of(0, 20));
        assertEquals(1, unreadPage.getTotalElements());
        assertEquals("Unread Alert", unreadPage.getContent().getFirst().getTitle());

        Page<Notification> readPage = notificationRepository.findAllByUserIdAndRead(
                userId, true, PageRequest.of(0, 20));
        assertEquals(1, readPage.getTotalElements());
    }

    @Test
    void countByUserIdAndReadFalse_returnsUnreadCount() {
        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        assertEquals(1, count);
    }

    @Test
    void countByUserIdAndReadFalse_returnsZeroWhenAllRead() {
        readNotification.setRead(true);
        unreadNotification.setRead(true);
        notificationRepository.save(readNotification);
        notificationRepository.save(unreadNotification);

        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        assertEquals(0, count);
    }

    @Test
    void findAllByUserIdAndTypeAndReadFalse_returnsUnreadByType() {
        var results = notificationRepository.findAllByUserIdAndTypeAndReadFalse(
                userId, NotificationType.SYSTEM);
        assertEquals(1, results.size());
    }
}
