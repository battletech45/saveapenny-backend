package com.saveapenny.assistant.service.impl;

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
import com.saveapenny.assistant.prompt.FinancePromptBuilder.PromptPayload;
import com.saveapenny.assistant.repository.AssistantChatMessageRepository;
import com.saveapenny.assistant.repository.AssistantChatSessionRepository;
import com.saveapenny.assistant.service.AssistantService;
import com.saveapenny.assistant.tool.AssistantBudgetTool;
import com.saveapenny.assistant.tool.AssistantReportTool;
import com.saveapenny.assistant.tool.AssistantToolContextHolder;
import com.saveapenny.assistant.tool.AssistantTransactionTool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssistantServiceImpl implements AssistantService {

    private static final String DISCLAIMER =
            "This assistant provides general budgeting guidance, not financial, tax, or legal advice.";

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final AssistantProperties assistantProperties;
    private final FinancePromptBuilder financePromptBuilder;
    private final AssistantReportTool assistantReportTool;
    private final AssistantBudgetTool assistantBudgetTool;
    private final AssistantTransactionTool assistantTransactionTool;
    private final AssistantToolContextHolder assistantToolContextHolder;
    private final AssistantChatSessionRepository assistantChatSessionRepository;
    private final AssistantChatMessageRepository assistantChatMessageRepository;

    public AssistantServiceImpl(
            ObjectProvider<ChatClient> chatClientProvider,
            AssistantProperties assistantProperties,
            FinancePromptBuilder financePromptBuilder,
            AssistantReportTool assistantReportTool,
            AssistantBudgetTool assistantBudgetTool,
            AssistantTransactionTool assistantTransactionTool,
            AssistantToolContextHolder assistantToolContextHolder,
            AssistantChatSessionRepository assistantChatSessionRepository,
            AssistantChatMessageRepository assistantChatMessageRepository) {
        this.chatClientProvider = chatClientProvider;
        this.assistantProperties = assistantProperties;
        this.financePromptBuilder = financePromptBuilder;
        this.assistantReportTool = assistantReportTool;
        this.assistantBudgetTool = assistantBudgetTool;
        this.assistantTransactionTool = assistantTransactionTool;
        this.assistantToolContextHolder = assistantToolContextHolder;
        this.assistantChatSessionRepository = assistantChatSessionRepository;
        this.assistantChatMessageRepository = assistantChatMessageRepository;
    }

    @Override
    public AssistantChatResponse chat(UUID userId, AssistantChatRequest request) {
        if (!assistantProperties.enabled()) {
            throw new AssistantDisabledException();
        }

        try {
            AssistantChatSession session = resolveSession(userId, request);
            ChatClient chatClient = resolveChatClient();
            List<AssistantMessageDto> history = resolveHistory(session.getId());
            List<AssistantMessageDto> trimmedHistory = trimHistory(history);
            String systemPrompt = assistantProperties.systemPrompt()
                    + "\nUse available tools for user-specific financial facts such as summaries, budget status, category spending, and recent transactions."
                    + " Do not invent balances, totals, or trends when tool data is available.";
            PromptPayload payload = financePromptBuilder.build(
                    systemPrompt,
                    trimmedHistory,
                    request.getMessage());

            assistantToolContextHolder.setCurrentUserId(userId);
            String reply = chatClient.prompt(toPrompt(payload))
                    .tools(assistantReportTool, assistantBudgetTool, assistantTransactionTool)
                    .call()
                    .content();

            session = persistReply(session, request.getMessage(), reply, userId);

            return AssistantChatResponse.builder()
                    .sessionId(session.getId())
                    .reply(reply)
                    .disclaimer(DISCLAIMER)
                    .build();
        } catch (AssistantDisabledException | AssistantChatSessionNotFoundException | AssistantProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AssistantProcessingException(
                    "Assistant response generation failed for userId=" + userId,
                    ex);
        } finally {
            assistantToolContextHolder.clear();
        }
    }

    private AssistantChatSession resolveSession(UUID userId, AssistantChatRequest request) {
        if (request.getSessionId() == null) {
            return AssistantChatSession.builder()
                    .userId(userId)
                    .title(buildSessionTitle(request.getMessage()))
                    .build();
        }

        return assistantChatSessionRepository.findByIdAndUserId(request.getSessionId(), userId)
                .orElseThrow(() -> new AssistantChatSessionNotFoundException(request.getSessionId()));
    }

    private String buildSessionTitle(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
    }

    private List<AssistantMessageDto> resolveHistory(UUID sessionId) {
        if (sessionId != null) {
            return resolvePersistedHistory(sessionId);
        }

        return List.of();
    }

    private List<AssistantMessageDto> resolvePersistedHistory(UUID sessionId) {
        
        int maxHistory = Math.max(0, assistantProperties.maxHistory());
        if (maxHistory == 0) {
            return List.of();
        }

        List<AssistantChatMessage> persistedMessages = new ArrayList<>(assistantChatMessageRepository
                .findAllBySessionIdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, maxHistory)));
        Collections.reverse(persistedMessages);

        return persistedMessages.stream()
                .map(message -> AssistantMessageDto.builder()
                        .role(message.getRole())
                        .content(message.getContent())
                        .build())
                .toList();
    }

    private AssistantChatSession persistMessages(AssistantChatSession session, String userMessage, String assistantReply) {
        if (session.getId() == null) {
            session = assistantChatSessionRepository.save(session);
        }
        assistantChatMessageRepository.save(AssistantChatMessage.builder()
                .sessionId(session.getId())
                .role("user")
                .content(userMessage)
                .build());
        assistantChatMessageRepository.save(AssistantChatMessage.builder()
                .sessionId(session.getId())
                .role("assistant")
                .content(assistantReply)
                .build());
        assistantChatSessionRepository.save(session);
        return session;
    }

    private AssistantChatSession persistReply(
            AssistantChatSession session,
            String userMessage,
            String assistantReply,
            UUID userId) {
        try {
            return persistMessages(session, userMessage, assistantReply);
        } catch (RuntimeException ex) {
            throw new AssistantProcessingException(
                    "Assistant reply persistence failed for userId=" + userId,
                    ex);
        }
    }

    private ChatClient resolveChatClient() {
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            throw new IllegalStateException("Assistant chat client is not configured.");
        }
        return chatClient;
    }

    private List<AssistantMessageDto> trimHistory(List<AssistantMessageDto> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        int maxHistory = Math.max(0, assistantProperties.maxHistory());
        if (maxHistory == 0) {
            return List.of();
        }

        int fromIndex = Math.max(0, history.size() - maxHistory);
        return List.copyOf(history.subList(fromIndex, history.size()));
    }

    private Prompt toPrompt(PromptPayload payload) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(payload.systemPrompt()));

        for (AssistantMessageDto historyMessage : payload.history()) {
            messages.add(toMessage(historyMessage));
        }

        messages.add(new UserMessage(payload.userMessage()));
        return new Prompt(messages);
    }

    private Message toMessage(AssistantMessageDto message) {
        return switch (message.getRole()) {
            case "assistant" -> new AssistantMessage(message.getContent());
            case "user" -> new UserMessage(message.getContent());
            default -> new UserMessage(message.getContent());
        };
    }
}
