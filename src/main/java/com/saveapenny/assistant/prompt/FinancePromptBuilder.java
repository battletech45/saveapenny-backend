package com.saveapenny.assistant.prompt;

import com.saveapenny.assistant.dto.AssistantMessageDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FinancePromptBuilder {

    public PromptPayload build(String systemPrompt, List<AssistantMessageDto> history, String userMessage) {
        return new PromptPayload(
                normalize(systemPrompt),
                history == null ? List.of() : history.stream()
                        .filter(message -> isSupportedRole(normalize(message.getRole())))
                        .map(message -> AssistantMessageDto.builder()
                                .role(normalize(message.getRole()))
                                .content(normalize(message.getContent()))
                                .build())
                        .toList(),
                normalize(userMessage));
    }

    private boolean isSupportedRole(String role) {
        return "user".equals(role) || "assistant".equals(role);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record PromptPayload(
            String systemPrompt,
            List<AssistantMessageDto> history,
            String userMessage) {
    }
}
