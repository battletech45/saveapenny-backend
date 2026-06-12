package com.saveapenny.config.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int maxRequestsPerMinute) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(maxRequestsPerMinute));
        return bucket.tryConsume();
    }

    public void cleanUp() {
        buckets.clear();
    }

    static class TokenBucket {
        private final int maxTokens;
        private final AtomicInteger tokens;
        private volatile long lastRefill;

        TokenBucket(int maxTokens) {
            this.maxTokens = maxTokens;
            this.tokens = new AtomicInteger(maxTokens);
            this.lastRefill = System.currentTimeMillis();
        }

        boolean tryConsume() {
            refill();
            int current = tokens.get();
            if (current <= 0) {
                return false;
            }
            return tokens.compareAndSet(current, current - 1);
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefill;
            if (elapsed < 60_000) {
                return;
            }
            synchronized (this) {
                if (now - lastRefill < 60_000) {
                    return;
                }
                tokens.set(maxTokens);
                lastRefill = now;
            }
        }
    }
}
