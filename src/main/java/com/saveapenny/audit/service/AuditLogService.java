package com.saveapenny.audit.service;

import com.saveapenny.audit.dto.AuditLogFilterRequest;
import com.saveapenny.audit.dto.AuditLogResponse;
import com.saveapenny.audit.dto.CreateAuditLogRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    AuditLogResponse create(UUID currentUserId, CreateAuditLogRequest request);

    Page<AuditLogResponse> getAll(UUID currentUserId, AuditLogFilterRequest filter, Pageable pageable);

    AuditLogResponse getById(UUID currentUserId, UUID auditLogId);
}
