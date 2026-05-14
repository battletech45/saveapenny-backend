package com.saveapenny.audit.dto;

import com.saveapenny.audit.entity.AuditAction;
import com.saveapenny.audit.entity.AuditEntityType;
import jakarta.validation.constraints.NotNull;
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
public class CreateAuditLogRequest {

    @NotNull
    private AuditAction action;

    @NotNull
    private AuditEntityType entityType;

    @NotNull
    private UUID entityId;

    private String oldValue;
    private String newValue;
}
