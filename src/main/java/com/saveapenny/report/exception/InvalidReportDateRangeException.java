package com.saveapenny.report.exception;

import java.time.LocalDate;

public class InvalidReportDateRangeException extends RuntimeException {

    public InvalidReportDateRangeException(LocalDate from, LocalDate to) {
        super("Invalid report date range: from=" + from + ", to=" + to);
    }
}
