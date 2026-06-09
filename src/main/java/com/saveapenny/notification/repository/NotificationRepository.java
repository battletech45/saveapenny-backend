package com.saveapenny.notification.repository;

import com.saveapenny.notification.entity.Notification;
import com.saveapenny.notification.entity.NotificationType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    Page<Notification> findAllByUserId(UUID userId, Pageable pageable);

    Page<Notification> findAllByUserIdAndRead(UUID userId, Boolean read, Pageable pageable);

    List<Notification> findAllByUserIdAndTypeAndReadFalse(UUID userId, NotificationType type);

    long countByUserIdAndReadFalse(UUID userId);
}
