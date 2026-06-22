package com.saveapenny.audit.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.audit.entity.AuditAction;
import com.saveapenny.audit.entity.AuditEntityType;
import com.saveapenny.audit.entity.AuditLog;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private UUID userId;
    private UUID entityId;
    private AuditLog log1;
    private AuditLog log2;
    private AuditLog log3;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        entityId = UUID.randomUUID();

        log1 = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action(AuditAction.CREATE)
                .entityType(AuditEntityType.ACCOUNT)
                .entityId(entityId)
                .newValue("{\"name\":\"Wallet\"}")
                .createdAt(OffsetDateTime.now().minusDays(2))
                .build();

        log2 = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action(AuditAction.UPDATE)
                .entityType(AuditEntityType.ACCOUNT)
                .entityId(entityId)
                .oldValue("{\"name\":\"Wallet\"}")
                .newValue("{\"name\":\"Savings\"}")
                .createdAt(OffsetDateTime.now().minusDays(1))
                .build();

        log3 = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .action(AuditAction.LOGIN)
                .entityType(AuditEntityType.USER)
                .entityId(UUID.randomUUID())
                .createdAt(OffsetDateTime.now())
                .build();

        auditLogRepository.save(log1);
        auditLogRepository.save(log2);
        auditLogRepository.save(log3);
    }

    @Test
    void findAllByUserId_returnsUserLogs() {
        Page<AuditLog> page = auditLogRepository.findAllByUserId(userId, PageRequest.of(0, 20));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findAllByUserId_returnsEmptyForUnknownUser() {
        Page<AuditLog> page = auditLogRepository.findAllByUserId(UUID.randomUUID(), PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByUserIdAndCreatedAtBetween_filtersByDateRange() {
        OffsetDateTime from = OffsetDateTime.now().minusDays(3);
        OffsetDateTime to = OffsetDateTime.now().minusHours(12);
        Page<AuditLog> page = auditLogRepository.findAllByUserIdAndCreatedAtBetween(
                userId, from, to, PageRequest.of(0, 20));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndCreatedAtBetween_excludesOutsideRange() {
        OffsetDateTime from = OffsetDateTime.now().plusDays(1);
        OffsetDateTime to = OffsetDateTime.now().plusDays(2);
        Page<AuditLog> page = auditLogRepository.findAllByUserIdAndCreatedAtBetween(
                userId, from, to, PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByEntityTypeAndEntityId_returnsMatchingLogs() {
        Page<AuditLog> page = auditLogRepository.findAllByEntityTypeAndEntityId(
                AuditEntityType.ACCOUNT, entityId, PageRequest.of(0, 20));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findAllByEntityTypeAndEntityId_returnsEmptyForNoMatch() {
        Page<AuditLog> page = auditLogRepository.findAllByEntityTypeAndEntityId(
                AuditEntityType.TRANSACTION, UUID.randomUUID(), PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByUserIdAndEntityType_filtersByType() {
        Page<AuditLog> page = auditLogRepository.findAllByUserIdAndEntityType(
                userId, AuditEntityType.ACCOUNT, PageRequest.of(0, 20));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndEntityType_excludesWrongType() {
        Page<AuditLog> page = auditLogRepository.findAllByUserIdAndEntityType(
                userId, AuditEntityType.USER, PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByUserIdAndEntityTypeAndEntityId_narrowestFilter() {
        Page<AuditLog> page = auditLogRepository.findAllByUserIdAndEntityTypeAndEntityId(
                userId, AuditEntityType.ACCOUNT, entityId, PageRequest.of(0, 20));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndEntityTypeAndEntityId_returnsEmptyOnMismatch() {
        Page<AuditLog> page = auditLogRepository.findAllByUserIdAndEntityTypeAndEntityId(
                userId, AuditEntityType.ACCOUNT, UUID.randomUUID(), PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }
}
