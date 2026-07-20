package com.saveapenny.push.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "push.fcm")
public record PushProperties(
        boolean enabled,
        String projectId,
        String clientEmail,
        String privateKey,
        String tokenUri,
        String fcmEndpointTemplate,
        long timeoutMillis) {
}
