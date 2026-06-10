package com.saveapenny.config.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
    }

    @Test
    void tryConsume_returnsTrue_whenTokensAvailable() {
        assertTrue(rateLimiter.tryConsume("key-1", 3));
    }

    @Test
    void tryConsume_returnsFalse_whenTokensExhausted() {
        String key = "key-2";
        assertTrue(rateLimiter.tryConsume(key, 2));
        assertTrue(rateLimiter.tryConsume(key, 2));
        assertFalse(rateLimiter.tryConsume(key, 2));
    }

    @Test
    void tryConsume_usesSeparateBucketsPerKey() {
        assertTrue(rateLimiter.tryConsume("key-a", 1));
        assertFalse(rateLimiter.tryConsume("key-a", 1));
        assertTrue(rateLimiter.tryConsume("key-b", 1));
    }

    @Test
    void cleanUp_clearsAllBuckets() {
        rateLimiter.tryConsume("key-3", 1);
        rateLimiter.cleanUp();
        assertTrue(rateLimiter.tryConsume("key-3", 1));
    }

    @Test
    void differentLimits_createSeparateBuckets() {
        assertTrue(rateLimiter.tryConsume("key-4", 5));
        assertTrue(rateLimiter.tryConsume("key-4", 5));
        assertTrue(rateLimiter.tryConsume("key-4", 5));
        assertTrue(rateLimiter.tryConsume("key-4", 5));
        assertTrue(rateLimiter.tryConsume("key-4", 5));
        assertFalse(rateLimiter.tryConsume("key-4", 5));
    }

    @Test
    void tokenBucket_constructor_setsInitialTokens() {
        var bucket = new RateLimiter.TokenBucket(10);
        // first call should succeed
        assertTrue(bucket.tryConsume());
    }
}
