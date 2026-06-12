package com.saveapenny.audit.service.impl;

import com.saveapenny.audit.dto.AuditLogFilterRequest;
import com.saveapenny.audit.dto.AuditLogResponse;
import com.saveapenny.audit.dto.CreateAuditLogRequest;
import com.saveapenny.audit.entity.AuditLog;
import com.saveapenny.audit.exception.AuditLogAccessDeniedException;
import com.saveapenny.audit.exception.AuditLogNotFoundException;
import com.saveapenny.audit.exception.InvalidAuditDateRangeException;
import com.saveapenny.audit.mapper.AuditLogMapper;
import com.saveapenny.audit.repository.AuditLogRepository;
import com.saveapenny.audit.service.AuditLogService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public AuditLogResponse create(UUID currentUserId, CreateAuditLogRequest request) {
        AuditLog auditLog = auditLogMapper.toEntity(request);
        auditLog.setUserId(currentUserId);
        AuditLog saved = auditLogRepository.save(auditLog);
        return auditLogMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAll(UUID currentUserId, AuditLogFilterRequest filter, Pageable pageable) {
        if (filter == null) {
            return auditLogRepository.findAllByUserId(currentUserId, pageable).map(auditLogMapper::toResponse);
        }

        OffsetDateTime from = filter.getFrom();
        OffsetDateTime to = filter.getTo();
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new InvalidAuditDateRangeException(from, to);
            }
            return auditLogRepository.findAllByUserIdAndCreatedAtBetween(currentUserId, from, to, pageable)
                    .map(auditLogMapper::toResponse);
        }
        if (from != null) {
            return auditLogRepository.findAllByUserIdAndCreatedAtGreaterThanEqual(currentUserId, from, pageable)
                    .map(auditLogMapper::toResponse);
        }
        if (to != null) {
            return auditLogRepository.findAllByUserIdAndCreatedAtLessThanEqual(currentUserId, to, pageable)
                    .map(auditLogMapper::toResponse);
        }

        if (filter.getEntityType() != null && filter.getEntityId() != null) {
            return auditLogRepository.findAllByUserIdAndEntityTypeAndEntityId(
                            currentUserId,
                            filter.getEntityType(),
                            filter.getEntityId(),
                            pageable)
                    .map(auditLogMapper::toResponse);
        }

        if (filter.getEntityType() != null) {
            return auditLogRepository.findAllByUserIdAndEntityType(currentUserId, filter.getEntityType(), pageable)
                    .map(auditLogMapper::toResponse);
        }

        return auditLogRepository.findAllByUserId(currentUserId, pageable).map(auditLogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getById(UUID currentUserId, UUID auditLogId) {
        AuditLog auditLog = auditLogRepository.findById(auditLogId)
                .orElseThrow(() -> new AuditLogNotFoundException(auditLogId));
        if (!auditLog.getUserId().equals(currentUserId)) {
            throw new AuditLogAccessDeniedException(auditLogId);
        }
        return auditLogMapper.toResponse(auditLog);
    }
}
