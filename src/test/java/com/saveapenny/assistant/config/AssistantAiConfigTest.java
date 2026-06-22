package com.saveapenny.assistant.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class AssistantAiConfigTest {

    private final AssistantAiConfig config = new AssistantAiConfig();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void assistantChatClient_supportsOpenAiProvider() {
        ChatClient chatClient = config.assistantChatClient(
                assistantProperties("openai"),
                "openai-key",
                ObservationRegistry.NOOP,
                meterRegistry);

        assertNotNull(chatClient);
    }

    @Test
    void assistantChatClient_supportsOpenRouterProvider() {
        ChatClient chatClient = config.assistantChatClient(
                assistantProperties("openrouter"),
                "",
                ObservationRegistry.NOOP,
                meterRegistry);

        assertNotNull(chatClient);
    }

    @Test
    void assistantChatClient_supportsOpenRouterWithoutSiteUrl() {
        AssistantProperties properties = new AssistantProperties(
                true,
                8,
                "poolside/laguna-xs.2:free",
                "Finance system prompt",
                "openrouter",
                "openrouter-key",
                "https://openrouter.ai/api",
                "",
                "SaveAPenny");

        assertDoesNotThrow(() -> config.assistantChatClient(
                properties,
                "",
                ObservationRegistry.NOOP,
                meterRegistry));
    }

    @Test
    void assistantChatClient_usesDefaultModelWhenBlank() {
        AssistantProperties properties = new AssistantProperties(
                true,
                8,
                " ",
                "Finance system prompt",
                "openai",
                "",
                "https://openrouter.ai/api",
                "",
                "SaveAPenny");

        ChatClient chatClient = config.assistantChatClient(
                properties,
                "openai-key",
                ObservationRegistry.NOOP,
                meterRegistry);

        assertNotNull(chatClient);
    }

    @Test
    void assistantChatClient_rejectsUnsupportedProvider() {
        assertThrows(
                IllegalStateException.class,
                () -> config.assistantChatClient(
                        assistantProperties("unsupported"),
                        "openai-key",
                        ObservationRegistry.NOOP,
                        meterRegistry));
    }

    private AssistantProperties assistantProperties(String provider) {
        return new AssistantProperties(
                true,
                8,
                "poolside/laguna-xs.2:free",
                "Finance system prompt",
                provider,
                "openrouter-key",
                "https://openrouter.ai/api",
                "https://saveapenny.example",
                "SaveAPenny");
    }
}
