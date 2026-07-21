package com.saveapenny.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "revenuecat")
public record RevenueCatProperties(
        boolean enabled,
        String secretApiKey,
        String baseUrl,
        String entitlementIdentifier) {
}
