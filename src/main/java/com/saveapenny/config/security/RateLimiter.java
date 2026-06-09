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
            if (elapsed < 1_000) {
                return;
            }
            synchronized (this) {
                long elapsedSync = now - lastRefill;
                if (elapsedSync < 1_000) {
                    return;
                }
                int refillCount = (int) (elapsedSync / 60_000);
                if (refillCount > 0) {
                    int newTokens = Math.min(maxTokens, tokens.get() + refillCount * maxTokens);
                    tokens.set(newTokens);
                    lastRefill = now;
                }
            }
        }
    }
}
