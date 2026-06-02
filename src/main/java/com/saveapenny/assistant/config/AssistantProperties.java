package com.saveapenny.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant")
public record AssistantProperties(
        boolean enabled,
        int maxHistory,
        String systemPrompt) {
}
