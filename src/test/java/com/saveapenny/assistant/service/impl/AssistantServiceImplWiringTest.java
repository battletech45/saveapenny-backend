package com.saveapenny.assistant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.assistant.config.AssistantProperties;
import com.saveapenny.assistant.dto.AssistantChatRequest;
import com.saveapenny.assistant.dto.AssistantChatResponse;
import com.saveapenny.assistant.entity.AssistantChatMessage;
import com.saveapenny.assistant.entity.AssistantChatSession;
import com.saveapenny.assistant.prompt.FinancePromptBuilder;
import com.saveapenny.assistant.repository.AssistantChatMessageRepository;
import com.saveapenny.assistant.repository.AssistantChatSessionRepository;
import com.saveapenny.assistant.tool.AssistantToolContextHolder;
import com.saveapenny.mcp.adapter.springai.SpringAiMcpToolAdapter;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AssistantServiceImplWiringTest.TestConfig.class)
class AssistantServiceImplWiringTest {

    @jakarta.annotation.Resource
    private AssistantServiceImpl assistantService;

    @jakarta.annotation.Resource(name = "assistantChatClient")
    private ChatClient assistantChatClient;

    @jakarta.annotation.Resource(name = "insightChatClient")
    private ChatClient insightChatClient;

    @jakarta.annotation.Resource
    private AssistantChatSessionRepository assistantChatSessionRepository;

    @jakarta.annotation.Resource
    private AssistantChatMessageRepository assistantChatMessageRepository;

    @Test
    void chat_usesAssistantChatClientWhenMultipleChatClientsExist() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AssistantChatSession session = AssistantChatSession.builder()
                .id(sessionId)
                .userId(userId)
                .title("Need help saving")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        ChatClient.ChatClientRequestSpec requestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class, Mockito.RETURNS_DEEP_STUBS);

        when(assistantChatSessionRepository.save(any(AssistantChatSession.class))).thenReturn(session);
        when(assistantChatMessageRepository.save(any(AssistantChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assistantChatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call().content()).thenReturn("Try a stricter weekly dining cap.");

        AssistantChatResponse response = assistantService.chat(userId, AssistantChatRequest.builder()
                .message("How can I save more?")
                .build());

        assertEquals(sessionId, response.getSessionId());
        assertEquals("Try a stricter weekly dining cap.", response.getReply());
        verify(assistantChatClient).prompt(any(Prompt.class));
        verify(insightChatClient, never()).prompt(any(Prompt.class));
    }

    @Configuration
    @Import(AssistantServiceImpl.class)
    static class TestConfig {

        @Bean("assistantChatClient")
        ChatClient assistantChatClient() {
            return Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        }

        @Bean("insightChatClient")
        ChatClient insightChatClient() {
            return Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        }

        @Bean
        AssistantProperties assistantProperties() {
            return new AssistantProperties(
                    true,
                    8,
                    "gpt-4.1-mini",
                    "Finance system prompt",
                    "openai",
                    "",
                    "https://openrouter.ai/api",
                    "",
                    "SaveAPenny");
        }

        @Bean
        FinancePromptBuilder financePromptBuilder() {
            return new FinancePromptBuilder();
        }

        @Bean
        SpringAiMcpToolAdapter springAiMcpToolAdapter() {
            return Mockito.mock(SpringAiMcpToolAdapter.class);
        }

        @Bean
        AssistantToolContextHolder assistantToolContextHolder() {
            return Mockito.mock(AssistantToolContextHolder.class);
        }

        @Bean
        AssistantChatSessionRepository assistantChatSessionRepository() {
            return Mockito.mock(AssistantChatSessionRepository.class);
        }

        @Bean
        AssistantChatMessageRepository assistantChatMessageRepository() {
            return Mockito.mock(AssistantChatMessageRepository.class);
        }
    }
}
