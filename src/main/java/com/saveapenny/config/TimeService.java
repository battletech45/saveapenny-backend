package com.saveapenny.config;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeService {

    private final Clock clock;

    public TimeService(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public Instant now() {
        return Instant.now(clock);
    }

    public LocalDateTime currentDateTime() {
        return LocalDateTime.now(clock);
    }

    public ZonedDateTime currentZonedDateTime() {
        return ZonedDateTime.now(clock);
    }

    public Clock getClock() {
        return clock;
    }
}
