package com.saveapenny.insight.config;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConditionalOnProperty(prefix = "insight", name = "ai-enhanced", havingValue = "true")
public class InsightAiConfig {

    @Bean("insightChatClient")
    public ChatClient insightChatClient(
            InsightProperties insightProperties,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder,
            ObservationRegistry observationRegistry) {
        OpenAiApi openAiApi = buildOpenAiApi(
                insightProperties,
                apiKey,
                restClientBuilder,
                webClientBuilder);

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .model(resolveModel(insightProperties.model()))
                .build();

        ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder()
                .observationRegistry(observationRegistry)
                .toolCallbackResolver(new StaticToolCallbackResolver(List.of()))
                .toolExecutionExceptionProcessor(new DefaultToolExecutionExceptionProcessor(false))
                .build();

        OpenAiChatModel chatModel = new OpenAiChatModel(
                openAiApi,
                chatOptions,
                toolCallingManager,
                RetryTemplate.defaultInstance(),
                observationRegistry);

        return ChatClient.create(chatModel);
    }

    OpenAiApi buildOpenAiApi(
            InsightProperties insightProperties,
            String openAiApiKey,
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder) {
        String provider = normalizeProvider(insightProperties.provider());
        OpenAiApi.Builder builder = OpenAiApi.builder()
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder);

        return switch (provider) {
            case "openai" -> builder
                    .apiKey(requireApiKey(openAiApiKey, "spring.ai.openai.api-key"))
                    .build();
            case "openrouter" -> builder
                    .apiKey(requireApiKey(insightProperties.openrouterApiKey(), "insight.openrouter-api-key"))
                    .baseUrl(insightProperties.openrouterBaseUrl())
                    .completionsPath("/v1/chat/completions")
                    .embeddingsPath("/v1/embeddings")
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
}
