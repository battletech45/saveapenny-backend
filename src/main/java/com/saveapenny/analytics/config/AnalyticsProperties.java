package com.saveapenny.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase.analytics")
public record AnalyticsProperties(
        boolean enabled,
        String androidAppId,
        String androidApiSecret,
        String iosAppId,
        String iosApiSecret,
        String endpoint,
        String debugEndpoint,
        boolean validateOnly,
        long timeoutMillis) {
}
