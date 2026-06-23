package com.saveapenny.stock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stock")
public record StockProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        int rateLimitPerMinute,
        int rateLimitPerDay) {
}
