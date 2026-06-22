package com.saveapenny.insight.config;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.setup.OpenAiSetup;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(prefix = "insight", name = "ai-enhanced", havingValue = "true")
public class InsightAiConfig {

    @Bean("insightChatClient")
    public ChatClient insightChatClient(
            InsightProperties insightProperties,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        OpenAIClient openAiClient = buildOpenAiClient(
                insightProperties,
                apiKey,
                observationRegistry,
                meterRegistry);
        OpenAIClientAsync openAiClientAsync = buildOpenAiClientAsync(
                insightProperties,
                apiKey,
                observationRegistry,
                meterRegistry);

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .model(resolveModel(insightProperties.model()))
                .build();

        ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder()
                .observationRegistry(observationRegistry)
                .toolCallbackResolver(new StaticToolCallbackResolver(List.of()))
                .toolExecutionExceptionProcessor(new DefaultToolExecutionExceptionProcessor(false))
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiClient(openAiClient)
                .openAiClientAsync(openAiClientAsync)
                .options(chatOptions)
                .toolCallingManager(toolCallingManager)
                .observationRegistry(observationRegistry)
                .meterRegistry(meterRegistry)
                .build();

        return ChatClient.create(chatModel);
    }

    OpenAIClient buildOpenAiClient(
            InsightProperties insightProperties,
            String openAiApiKey,
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        OpenAiChatOptions options = buildOpenAiChatOptions(insightProperties, openAiApiKey);
        return OpenAiSetup.setupSyncClient(
                options.getBaseUrl(),
                options.getApiKey(),
                options.getCredential(),
                options.getOrganizationId(),
                null,
                null,
                options.isMicrosoftFoundry(),
                options.isGitHubModels(),
                options.getPromptCacheKey(),
                options.getTimeout(),
                options.getMaxRetries(),
                options.getProxy(),
                options.getCustomHeaders(),
                observationRegistry,
                meterRegistry,
                List.of());
    }

    OpenAIClientAsync buildOpenAiClientAsync(
            InsightProperties insightProperties,
            String openAiApiKey,
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        OpenAiChatOptions options = buildOpenAiChatOptions(insightProperties, openAiApiKey);
        return OpenAiSetup.setupAsyncClient(
                options.getBaseUrl(),
                options.getApiKey(),
                options.getCredential(),
                options.getOrganizationId(),
                null,
                null,
                options.isMicrosoftFoundry(),
                options.isGitHubModels(),
                options.getPromptCacheKey(),
                options.getTimeout(),
                options.getMaxRetries(),
                options.getProxy(),
                options.getCustomHeaders(),
                observationRegistry,
                meterRegistry,
                List.of());
    }

    OpenAiChatOptions buildOpenAiChatOptions(
            InsightProperties insightProperties,
            String openAiApiKey) {
        String provider = normalizeProvider(insightProperties.provider());

        return switch (provider) {
            case "openai" -> OpenAiChatOptions.builder()
                    .apiKey(requireApiKey(openAiApiKey, "spring.ai.openai.api-key"))
                    .build();
            case "openrouter" -> OpenAiChatOptions.builder()
                    .apiKey(requireApiKey(insightProperties.openrouterApiKey(), "insight.openrouter-api-key"))
                    .baseUrl(resolveOpenRouterBaseUrl(insightProperties.openrouterBaseUrl()))
                    .build();
            default -> throw new IllegalStateException(
                    "Unsupported insight provider '" + provider + "'. Expected 'openai' or 'openrouter'.");
        };
    }

    private String normalizeProvider(String provider) {
        return StringUtils.hasText(provider) ? provider.trim().toLowerCase() : "openai";
    }

    private String resolveModel(String model) {
        return StringUtils.hasText(model) ? model.trim() : "gpt-4.1-mini";
    }

    private String requireApiKey(String apiKey, String propertyName) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Insight AI enhancement is enabled but " + propertyName + " is not configured.");
        }
        return apiKey;
    }

    private String resolveOpenRouterBaseUrl(String baseUrl) {
        String normalized = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://openrouter.ai/api";
        return normalized.endsWith("/v1") ? normalized : normalized + "/v1";
    }
}
