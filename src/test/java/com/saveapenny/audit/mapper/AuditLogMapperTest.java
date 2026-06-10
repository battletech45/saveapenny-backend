package com.saveapenny.audit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.saveapenny.audit.dto.AuditLogResponse;
import com.saveapenny.audit.dto.CreateAuditLogRequest;
import com.saveapenny.audit.entity.AuditAction;
import com.saveapenny.audit.entity.AuditEntityType;
import com.saveapenny.audit.entity.AuditLog;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AuditLogMapperTest {

    private final AuditLogMapper auditLogMapper = Mappers.getMapper(AuditLogMapper.class);

    @Test
    void toEntity_mapsCreateRequest() {
        UUID entityId = UUID.randomUUID();
        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .action(AuditAction.CREATE)
                .entityType(AuditEntityType.ACCOUNT)
                .entityId(entityId)
                .oldValue("old")
                .newValue("new")
                .build();

        AuditLog entity = auditLogMapper.toEntity(request);

        assertNull(entity.getId());
        assertNull(entity.getUserId());
        assertEquals(AuditAction.CREATE, entity.getAction());
        assertEquals(AuditEntityType.ACCOUNT, entity.getEntityType());
        assertEquals(entityId, entity.getEntityId());
        assertEquals("old", entity.getOldValue());
        assertEquals("new", entity.getNewValue());
        assertNull(entity.getCreatedAt());
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        AuditLog entity = AuditLog.builder()
                .id(id)
                .userId(userId)
                .action(AuditAction.DELETE)
                .entityType(AuditEntityType.TRANSACTION)
                .entityId(entityId)
                .oldValue("old-value")
                .newValue(null)
                .createdAt(now)
                .build();

        AuditLogResponse response = auditLogMapper.toResponse(entity);

        assertEquals(id, response.getId());
        assertEquals(userId, response.getUserId());
        assertEquals(AuditAction.DELETE, response.getAction());
        assertEquals(AuditEntityType.TRANSACTION, response.getEntityType());
        assertEquals(entityId, response.getEntityId());
        assertEquals("old-value", response.getOldValue());
        assertNull(response.getNewValue());
        assertEquals(now, response.getCreatedAt());
    }
}
