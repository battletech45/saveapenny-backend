package com.saveapenny.assistant.config;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Map;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
@ConditionalOnProperty(prefix = "assistant", name = "enabled", havingValue = "true")
public class AssistantAiConfig {

    @Bean
    public ChatClient assistantChatClient(
            AssistantProperties assistantProperties,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        OpenAIClient openAiClient = buildOpenAiClient(
                assistantProperties,
                apiKey,
                observationRegistry,
                meterRegistry);
        OpenAIClientAsync openAiClientAsync = buildOpenAiClientAsync(
                assistantProperties,
                apiKey,
                observationRegistry,
                meterRegistry);

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .model(resolveModel(assistantProperties.model()))
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
            AssistantProperties assistantProperties,
            String openAiApiKey,
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        OpenAiChatOptions options = buildOpenAiChatOptions(assistantProperties, openAiApiKey);
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
            AssistantProperties assistantProperties,
            String openAiApiKey,
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry) {
        OpenAiChatOptions options = buildOpenAiChatOptions(assistantProperties, openAiApiKey);
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
            AssistantProperties assistantProperties,
            String openAiApiKey) {
        String provider = normalizeProvider(assistantProperties.provider());

        return switch (provider) {
            case "openai" -> OpenAiChatOptions.builder()
                    .apiKey(requireApiKey(openAiApiKey, "spring.ai.openai.api-key"))
                    .build();
            case "openrouter" -> OpenAiChatOptions.builder()
                    .apiKey(requireApiKey(assistantProperties.openrouterApiKey(), "assistant.openrouter-api-key"))
                    .baseUrl(resolveOpenRouterBaseUrl(assistantProperties.openrouterBaseUrl()))
                    .customHeaders(toSingleValueMap(buildOpenRouterHeaders(assistantProperties)))
                    .build();
            default -> throw new IllegalStateException(
                    "Unsupported assistant provider '" + provider + "'. Expected 'openai' or 'openrouter'.");
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
            throw new IllegalStateException("Assistant is enabled but " + propertyName + " is not configured.");
        }
        return apiKey;
    }

    private MultiValueMap<String, String> buildOpenRouterHeaders(AssistantProperties assistantProperties) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        if (StringUtils.hasText(assistantProperties.openrouterSiteUrl())) {
            headers.add("HTTP-Referer", assistantProperties.openrouterSiteUrl().trim());
        }
        if (StringUtils.hasText(assistantProperties.openrouterAppName())) {
            headers.add("X-Title", assistantProperties.openrouterAppName().trim());
        }
        return headers;
    }

    private Map<String, String> toSingleValueMap(MultiValueMap<String, String> headers) {
        return headers.toSingleValueMap();
    }

    private String resolveOpenRouterBaseUrl(String baseUrl) {
        String normalized = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://openrouter.ai/api";
        return normalized.endsWith("/v1") ? normalized : normalized + "/v1";
    }
}
