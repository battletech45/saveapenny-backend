package com.saveapenny.insight.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "insight")
public record InsightProperties(
        boolean enabled,
        String cron,
        int maxInsightsPerGeneration,
        int deduplicationWindowDays,
        double stddevThreshold,
        double maxAmountRatio,
        boolean aiEnhanced,
        String model,
        String provider,
        String openrouterApiKey,
        String openrouterBaseUrl) {
}
