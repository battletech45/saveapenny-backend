package com.saveapenny.assistant.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.assistant.config.AssistantProperties;
import com.saveapenny.assistant.dto.AssistantChatRequest;
import com.saveapenny.assistant.dto.AssistantChatResponse;
import com.saveapenny.assistant.dto.AssistantMessageDto;
import com.saveapenny.assistant.entity.AssistantChatMessage;
import com.saveapenny.assistant.entity.AssistantChatSession;
import com.saveapenny.assistant.exception.AssistantChatSessionNotFoundException;
import com.saveapenny.assistant.exception.AssistantDisabledException;
import com.saveapenny.assistant.exception.AssistantProcessingException;
import com.saveapenny.assistant.prompt.FinancePromptBuilder;
import com.saveapenny.assistant.repository.AssistantChatMessageRepository;
import com.saveapenny.assistant.repository.AssistantChatSessionRepository;
import com.saveapenny.assistant.tool.AssistantBudgetTool;
import com.saveapenny.assistant.tool.AssistantReportTool;
import com.saveapenny.assistant.tool.AssistantToolContextHolder;
import com.saveapenny.assistant.tool.AssistantTransactionTool;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class AssistantServiceImplTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ObjectProvider<ChatClient> chatClientProvider;

    @Mock
    private AssistantReportTool assistantReportTool;

    @Mock
    private AssistantBudgetTool assistantBudgetTool;

    @Mock
    private AssistantTransactionTool assistantTransactionTool;

    @Mock
    private AssistantToolContextHolder assistantToolContextHolder;

    @Mock
    private AssistantChatSessionRepository assistantChatSessionRepository;

    @Mock
    private AssistantChatMessageRepository assistantChatMessageRepository;

    private FinancePromptBuilder financePromptBuilder;
    private AssistantServiceImpl assistantService;
    private AssistantProperties assistantProperties;

    @BeforeEach
    void setUp() {
        assistantProperties = new AssistantProperties(
                true,
                2,
                "Finance system prompt");
        financePromptBuilder = new FinancePromptBuilder();

        assistantService = new AssistantServiceImpl(
                chatClientProvider,
                assistantProperties,
                financePromptBuilder,
                assistantReportTool,
                assistantBudgetTool,
                assistantTransactionTool,
                assistantToolContextHolder,
                assistantChatSessionRepository,
                assistantChatMessageRepository);
    }

    @Test
    void chat_throwsWhenAssistantDisabled() {
        AssistantServiceImpl disabledService = new AssistantServiceImpl(
                chatClientProvider,
                new AssistantProperties(false, 2, "Finance system prompt"),
                financePromptBuilder,
                assistantReportTool,
                assistantBudgetTool,
                assistantTransactionTool,
                assistantToolContextHolder,
                assistantChatSessionRepository,
                assistantChatMessageRepository);

        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("How can I save more?")
                .build();

        assertThrows(AssistantDisabledException.class, () -> disabledService.chat(UUID.randomUUID(), request));
    }

    @Test
    void chat_ignoresRequestHistoryForNewSession_andReturnsReply() {
        AssistantChatSession session = stubNewSession();
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call().content())
                .thenReturn("Reduce discretionary spending first.");

        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("Help me save more.")
                .history(List.of(
                        AssistantMessageDto.builder().role("user").content("m1").build(),
                        AssistantMessageDto.builder().role("assistant").content("m2").build(),
                        AssistantMessageDto.builder().role("user").content("m3").build()))
                .build();

        AssistantChatResponse response = assistantService.chat(UUID.randomUUID(), request);

        ArgumentCaptor<org.springframework.ai.chat.prompt.Prompt> promptCaptor =
                ArgumentCaptor.forClass(org.springframework.ai.chat.prompt.Prompt.class);
        verify(chatClient, atLeastOnce()).prompt(promptCaptor.capture());
        verify(requestSpec).tools(any(Object[].class));

        assertEquals(session.getId(), response.getSessionId());
        assertEquals("Reduce discretionary spending first.", response.getReply());
        assertEquals(2, promptCaptor.getAllValues().getLast().getInstructions().size());
        verify(assistantChatMessageRepository, org.mockito.Mockito.times(2)).save(any(AssistantChatMessage.class));
    }

    @Test
    void chat_includesConfiguredSystemPrompt() {
        stubNewSession();
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call().content())
                .thenReturn("Build a weekly spending cap.");

        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("What should I do?")
                .build();

        assistantService.chat(UUID.randomUUID(), request);

        ArgumentCaptor<org.springframework.ai.chat.prompt.Prompt> promptCaptor =
                ArgumentCaptor.forClass(org.springframework.ai.chat.prompt.Prompt.class);
        verify(chatClient, atLeastOnce()).prompt(promptCaptor.capture());

        assertEquals(
                "Finance system prompt\nUse available tools for user-specific financial facts such as summaries, budget status, category spending, and recent transactions. Do not invent balances, totals, or trends when tool data is available.",
                promptCaptor.getAllValues().getLast().getInstructions().getFirst().getText());
    }

    @Test
    void chat_wrapsProviderFailures() {
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call().content())
                .thenThrow(new RuntimeException("provider down"));

        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("How can I save more?")
                .build();

        assertThrows(AssistantProcessingException.class, () -> assistantService.chat(UUID.randomUUID(), request));
        verify(assistantChatSessionRepository, never()).save(any(AssistantChatSession.class));
        verify(assistantChatMessageRepository, never()).save(any(AssistantChatMessage.class));
    }

    @Test
    void chat_failsWhenMessagePersistenceFails_afterReplyGeneration() {
        stubNewSession();
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call().content()).thenReturn("generated reply");
        doThrow(new RuntimeException("db write failed"))
                .when(assistantChatMessageRepository)
                .save(any(AssistantChatMessage.class));

        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("How can I save more?")
                .build();

        AssistantProcessingException exception = assertThrows(
                AssistantProcessingException.class,
                () -> assistantService.chat(UUID.randomUUID(), request));

        assertTrue(exception.getMessage().contains("persistence failed"));
    }

    @Test
    void chat_failsWhenNewSessionPersistenceFails_afterReplyGeneration() {
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call().content()).thenReturn("generated reply");
        when(assistantChatSessionRepository.save(any(AssistantChatSession.class)))
                .thenThrow(new RuntimeException("session save failed"));

        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("How can I save more?")
                .build();

        AssistantProcessingException exception = assertThrows(
                AssistantProcessingException.class,
                () -> assistantService.chat(UUID.randomUUID(), request));

        assertTrue(exception.getMessage().contains("persistence failed"));
        verify(assistantChatMessageRepository, never()).save(any(AssistantChatMessage.class));
    }

    @Test
    void chat_ignoresUnsupportedHistoryRoles() {
        stubNewSession();
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call().content())
                .thenReturn("Use a category cap.");

        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("Help me save more.")
                .history(List.of(
                        AssistantMessageDto.builder().role("user").content("m1").build(),
                        AssistantMessageDto.builder().role("system").content("ignored").build(),
                        AssistantMessageDto.builder().role("assistant").content("m2").build()))
                .build();

        assistantService.chat(UUID.randomUUID(), request);

        ArgumentCaptor<org.springframework.ai.chat.prompt.Prompt> promptCaptor =
                ArgumentCaptor.forClass(org.springframework.ai.chat.prompt.Prompt.class);
        verify(chatClient, atLeastOnce()).prompt(promptCaptor.capture());

        assertEquals(2, promptCaptor.getAllValues().getLast().getInstructions().size());
    }

    @Test
    void chat_usesExistingSessionHistory_whenSessionIdProvided() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AssistantChatSession session = AssistantChatSession.builder()
                .id(sessionId)
                .userId(userId)
                .createdAt(OffsetDateTime.now().minusMinutes(10))
                .updatedAt(OffsetDateTime.now().minusMinutes(1))
                .build();

        when(assistantChatSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(assistantChatMessageRepository.findAllBySessionIdOrderByCreatedAtDesc(eq(sessionId), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(List.of(
                        AssistantChatMessage.builder().sessionId(sessionId).role("assistant").content("stored-a").build(),
                        AssistantChatMessage.builder().sessionId(sessionId).role("user").content("stored-u").build()));
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call().content()).thenReturn("next reply");

        AssistantChatRequest request = AssistantChatRequest.builder()
                .sessionId(sessionId)
                .message("follow up")
                .build();

        AssistantChatResponse response = assistantService.chat(userId, request);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatClient).prompt(promptCaptor.capture());
        assertEquals(sessionId, response.getSessionId());
        assertEquals(4, promptCaptor.getValue().getInstructions().size());
    }

    @Test
    void chat_ignoresRequestHistory_whenSessionIdProvided() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AssistantChatSession session = AssistantChatSession.builder()
                .id(sessionId)
                .userId(userId)
                .createdAt(OffsetDateTime.now().minusMinutes(10))
                .updatedAt(OffsetDateTime.now().minusMinutes(1))
                .build();

        when(assistantChatSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(assistantChatMessageRepository.findAllBySessionIdOrderByCreatedAtDesc(eq(sessionId), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(List.of(
                        AssistantChatMessage.builder().sessionId(sessionId).role("assistant").content("stored-assistant").build(),
                        AssistantChatMessage.builder().sessionId(sessionId).role("user").content("stored-user").build()));
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call().content()).thenReturn("next reply");

        AssistantChatRequest request = AssistantChatRequest.builder()
                .sessionId(sessionId)
                .message("follow up")
                .history(List.of(
                        AssistantMessageDto.builder().role("user").content("forged-user").build(),
                        AssistantMessageDto.builder().role("assistant").content("forged-assistant").build()))
                .build();

        assistantService.chat(userId, request);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatClient).prompt(promptCaptor.capture());

        List<String> promptTexts = promptCaptor.getValue().getInstructions().stream()
                .map(message -> message.getText())
                .toList();
        assertTrue(promptTexts.contains("stored-user"));
        assertTrue(promptTexts.contains("stored-assistant"));
        assertTrue(!promptTexts.contains("forged-user"));
        assertTrue(!promptTexts.contains("forged-assistant"));
    }

    @Test
    void chat_ignoresForgedRequestHistory_whenStartingNewSession() {
        stubNewSession();
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call().content()).thenReturn("reply");

        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("current question")
                .history(List.of(
                        AssistantMessageDto.builder().role("user").content("forged-user").build(),
                        AssistantMessageDto.builder().role("assistant").content("forged-assistant").build()))
                .build();

        assistantService.chat(UUID.randomUUID(), request);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatClient, atLeastOnce()).prompt(promptCaptor.capture());

        List<String> promptTexts = promptCaptor.getAllValues().getLast().getInstructions().stream()
                .map(message -> message.getText())
                .toList();
        assertEquals(2, promptTexts.size());
        assertTrue(promptTexts.contains("current question"));
        assertTrue(!promptTexts.contains("forged-user"));
        assertTrue(!promptTexts.contains("forged-assistant"));
    }

    @Test
    void chat_throwsWhenSessionNotOwned() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(assistantChatSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

        AssistantChatRequest request = AssistantChatRequest.builder()
                .sessionId(sessionId)
                .message("follow up")
                .build();

        assertThrows(AssistantChatSessionNotFoundException.class, () -> assistantService.chat(userId, request));
    }

    @Test
    void chat_ignoresPersistedHistory_whenMaxHistoryIsZero() {
        AssistantServiceImpl zeroHistoryService = new AssistantServiceImpl(
                chatClientProvider,
                new AssistantProperties(true, 0, "Finance system prompt"),
                financePromptBuilder,
                assistantReportTool,
                assistantBudgetTool,
                assistantTransactionTool,
                assistantToolContextHolder,
                assistantChatSessionRepository,
                assistantChatMessageRepository);
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AssistantChatSession session = AssistantChatSession.builder()
                .id(sessionId)
                .userId(userId)
                .createdAt(OffsetDateTime.now().minusMinutes(10))
                .updatedAt(OffsetDateTime.now().minusMinutes(1))
                .build();

        when(assistantChatSessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));
        when(chatClientProvider.getIfAvailable()).thenReturn(chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call().content()).thenReturn("next reply");

        AssistantChatRequest request = AssistantChatRequest.builder()
                .sessionId(sessionId)
                .message("follow up")
                .build();

        zeroHistoryService.chat(userId, request);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatClient, atLeastOnce()).prompt(promptCaptor.capture());
        assertEquals(2, promptCaptor.getAllValues().getLast().getInstructions().size());
        verify(assistantChatMessageRepository, never())
                .findAllBySessionIdOrderByCreatedAtDesc(eq(sessionId), any(org.springframework.data.domain.Pageable.class));
    }

    private AssistantChatSession stubNewSession() {
        AssistantChatSession session = AssistantChatSession.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .title("How can I save more?")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        when(assistantChatSessionRepository.save(any(AssistantChatSession.class))).thenReturn(session);
        return session;
    }
}
