package com.saveapenny.audit.exception;

import java.util.UUID;

public class AuditLogAccessDeniedException extends RuntimeException {

    public AuditLogAccessDeniedException(UUID auditLogId) {
        super("Access denied for audit log: " + auditLogId);
    }
}
