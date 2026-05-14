package com.saveapenny.audit.exception;

import java.util.UUID;

public class AuditLogNotFoundException extends RuntimeException {

    public AuditLogNotFoundException(UUID auditLogId) {
        super("Audit log not found: " + auditLogId);
    }
}
