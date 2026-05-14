package com.saveapenny.audit.repository;

import com.saveapenny.audit.entity.AuditEntityType;
import com.saveapenny.audit.entity.AuditLog;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByUserId(UUID userId, Pageable pageable);

    Page<AuditLog> findAllByUserIdAndCreatedAtBetween(
            UUID userId,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable);

    Page<AuditLog> findAllByEntityTypeAndEntityId(
            AuditEntityType entityType,
            UUID entityId,
            Pageable pageable);

    Page<AuditLog> findAllByUserIdAndEntityType(
            UUID userId,
            AuditEntityType entityType,
            Pageable pageable);

    Page<AuditLog> findAllByUserIdAndEntityTypeAndEntityId(
            UUID userId,
            AuditEntityType entityType,
            UUID entityId,
            Pageable pageable);
}
