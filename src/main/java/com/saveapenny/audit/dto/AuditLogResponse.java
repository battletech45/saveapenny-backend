package com.saveapenny.audit.dto;

import com.saveapenny.audit.entity.AuditAction;
import com.saveapenny.audit.entity.AuditEntityType;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private UUID id;
    private UUID userId;
    private AuditAction action;
    private AuditEntityType entityType;
    private UUID entityId;
    private String oldValue;
    private String newValue;
    private OffsetDateTime createdAt;
}
