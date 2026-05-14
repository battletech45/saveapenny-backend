package com.saveapenny.audit.mapper;

import com.saveapenny.audit.dto.AuditLogResponse;
import com.saveapenny.audit.dto.CreateAuditLogRequest;
import com.saveapenny.audit.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    AuditLog toEntity(CreateAuditLogRequest request);

    AuditLogResponse toResponse(AuditLog auditLog);
}
