package com.saveapenny.push.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "push.fcm")
public record PushProperties(
        boolean enabled,
        String credentialsPath,
        String fcmEndpointTemplate,
        long timeoutMillis) {
}
