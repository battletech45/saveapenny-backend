package com.saveapenny.audit.exception;

import java.time.OffsetDateTime;

public class InvalidAuditDateRangeException extends RuntimeException {

    public InvalidAuditDateRangeException(OffsetDateTime from, OffsetDateTime to) {
        super("Invalid audit date range: from=" + from + ", to=" + to);
    }
}
