package com.saveapenny.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        Login login,
        Api api) {

    public record Login(int maxPerMinute) {}

    public record Api(int maxPerMinute) {}
}
