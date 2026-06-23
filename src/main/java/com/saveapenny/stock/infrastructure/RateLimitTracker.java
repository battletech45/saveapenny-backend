package com.saveapenny.stock.infrastructure;

import com.saveapenny.config.TimeService;
import com.saveapenny.stock.exception.StockRateLimitExceededException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RateLimitTracker {

    private final int maxPerMinute;
    private final int maxPerDay;
    private final TimeService timeService;
    private final Deque<Instant> minuteWindow = new ConcurrentLinkedDeque<>();
    private final Deque<Instant> dayWindow = new ConcurrentLinkedDeque<>();

    public RateLimitTracker(int maxPerMinute, int maxPerDay, TimeService timeService) {
        this.maxPerMinute = maxPerMinute;
        this.maxPerDay = maxPerDay;
        this.timeService = timeService;
    }

    public void checkQuota() {
        Instant now = timeService.now();
        evict(now);

        if (minuteWindow.size() >= maxPerMinute) {
            throw new StockRateLimitExceededException(
                    "Alpha Vantage rate limit exceeded: maximum " + maxPerMinute + " requests per minute");
        }
        if (dayWindow.size() >= maxPerDay) {
            throw new StockRateLimitExceededException(
                    "Alpha Vantage rate limit exceeded: maximum " + maxPerDay + " requests per day");
        }

        minuteWindow.addLast(now);
        dayWindow.addLast(now);
    }

    private void evict(Instant now) {
        Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
        Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);

        while (!minuteWindow.isEmpty() && minuteWindow.peekFirst().isBefore(oneMinuteAgo)) {
            minuteWindow.pollFirst();
        }
        while (!dayWindow.isEmpty() && dayWindow.peekFirst().isBefore(oneDayAgo)) {
            dayWindow.pollFirst();
        }
    }

    public int minuteWindowSize() {
        return minuteWindow.size();
    }

    public int dayWindowSize() {
        return dayWindow.size();
    }

    public void reset() {
        minuteWindow.clear();
        dayWindow.clear();
    }
}
