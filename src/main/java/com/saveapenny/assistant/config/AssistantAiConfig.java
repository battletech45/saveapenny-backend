package com.saveapenny.assistant.config;

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
@ConditionalOnProperty(prefix = "assistant", name = "enabled", havingValue = "true")
public class AssistantAiConfig {

    @Bean
    public ChatClient assistantChatClient(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:gpt-4.1-mini}") String model,
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder,
            ObservationRegistry observationRegistry) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Assistant is enabled but spring.ai.openai.api-key is not configured.");
        }

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .model(model)
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
}
