package com.saveapenny.audit.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.audit.dto.AuditLogFilterRequest;
import com.saveapenny.audit.dto.AuditLogResponse;
import com.saveapenny.audit.dto.CreateAuditLogRequest;
import com.saveapenny.audit.entity.AuditAction;
import com.saveapenny.audit.entity.AuditEntityType;
import com.saveapenny.audit.entity.AuditLog;
import com.saveapenny.audit.exception.AuditLogAccessDeniedException;
import com.saveapenny.audit.exception.AuditLogNotFoundException;
import com.saveapenny.audit.exception.InvalidAuditDateRangeException;
import com.saveapenny.audit.mapper.AuditLogMapper;
import com.saveapenny.audit.repository.AuditLogRepository;
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
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    private UUID userId;
    private UUID auditLogId;
    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        auditLogId = UUID.randomUUID();
        auditLog = AuditLog.builder()
                .id(auditLogId)
                .userId(userId)
                .action(AuditAction.UPDATE)
                .entityType(AuditEntityType.ACCOUNT)
                .entityId(UUID.randomUUID())
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void create_returnsResponse_whenValid() {
        CreateAuditLogRequest request = CreateAuditLogRequest.builder()
                .action(AuditAction.CREATE)
                .entityType(AuditEntityType.TRANSACTION)
                .entityId(UUID.randomUUID())
                .newValue("{\"amount\":10}")
                .build();

        AuditLog mapped = AuditLog.builder().action(AuditAction.CREATE).build();
        AuditLogResponse response = AuditLogResponse.builder().id(auditLogId).userId(userId).build();

        when(auditLogMapper.toEntity(request)).thenReturn(mapped);
        when(auditLogRepository.save(mapped)).thenReturn(auditLog);
        when(auditLogMapper.toResponse(auditLog)).thenReturn(response);

        AuditLogResponse result = auditLogService.create(userId, request);

        assertEquals(auditLogId, result.getId());
        assertEquals(userId, mapped.getUserId());
    }

    @Test
    void getAll_filtersByDateRange_whenProvided() {
        OffsetDateTime from = OffsetDateTime.now().minusDays(2);
        OffsetDateTime to = OffsetDateTime.now();
        AuditLogFilterRequest filter = AuditLogFilterRequest.builder().from(from).to(to).build();

        when(auditLogRepository.findAllByUserIdAndCreatedAtBetween(userId, from, to, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(java.util.List.of(auditLog)));
        when(auditLogMapper.toResponse(auditLog)).thenReturn(AuditLogResponse.builder().id(auditLogId).build());

        var result = auditLogService.getAll(userId, filter, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAll_throws_whenDateRangeInvalid() {
        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.minusDays(1);
        AuditLogFilterRequest filter = AuditLogFilterRequest.builder().from(from).to(to).build();

        assertThrows(InvalidAuditDateRangeException.class,
                () -> auditLogService.getAll(userId, filter, PageRequest.of(0, 20)));
    }

    @Test
    void getById_throws_whenNotFound() {
        when(auditLogRepository.findById(auditLogId)).thenReturn(Optional.empty());

        assertThrows(AuditLogNotFoundException.class, () -> auditLogService.getById(userId, auditLogId));
    }

    @Test
    void getById_throws_whenOwnedByAnotherUser() {
        AuditLog foreignLog = AuditLog.builder().id(auditLogId).userId(UUID.randomUUID()).build();
        when(auditLogRepository.findById(auditLogId)).thenReturn(Optional.of(foreignLog));

        assertThrows(AuditLogAccessDeniedException.class, () -> auditLogService.getById(userId, auditLogId));
    }

    @Test
    void getAll_filtersByEntityTypeAndEntityId_whenProvided() {
        UUID entityId = UUID.randomUUID();
        AuditLogFilterRequest filter = AuditLogFilterRequest.builder()
                .entityType(AuditEntityType.ACCOUNT)
                .entityId(entityId)
                .build();

        when(auditLogRepository.findAllByUserIdAndEntityTypeAndEntityId(
                userId, AuditEntityType.ACCOUNT, entityId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(java.util.List.of(auditLog)));
        when(auditLogMapper.toResponse(auditLog)).thenReturn(AuditLogResponse.builder().id(auditLogId).build());

        var result = auditLogService.getAll(userId, filter, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        verify(auditLogRepository).findAllByUserIdAndEntityTypeAndEntityId(
                userId, AuditEntityType.ACCOUNT, entityId, PageRequest.of(0, 20));
    }
}
