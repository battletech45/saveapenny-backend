package com.saveapenny.report.exception;

import java.time.LocalDate;

public class InvalidNetWorthSnapshotDateException extends RuntimeException {

    public InvalidNetWorthSnapshotDateException(LocalDate snapshotDate) {
        super("Invalid net worth snapshot date: " + snapshotDate);
    }
}
