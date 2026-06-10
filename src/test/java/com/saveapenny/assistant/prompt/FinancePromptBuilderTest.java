package com.saveapenny.assistant.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.assistant.dto.AssistantMessageDto;
import com.saveapenny.assistant.prompt.FinancePromptBuilder.PromptPayload;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinancePromptBuilderTest {

    private FinancePromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new FinancePromptBuilder();
    }

    @Test
    void build_createsPayloadWithAllParts() {
        List<AssistantMessageDto> history = List.of(
                AssistantMessageDto.builder().role("user").content("hello").build(),
                AssistantMessageDto.builder().role("assistant").content("hi there").build());

        PromptPayload payload = builder.build("You are a finance bot.", history, "My latest question");

        assertEquals("You are a finance bot.", payload.systemPrompt());
        assertEquals(2, payload.history().size());
        assertEquals("My latest question", payload.userMessage());
    }

    @Test
    void build_filtersUnsupportedRoles() {
        List<AssistantMessageDto> history = List.of(
                AssistantMessageDto.builder().role("user").content("hello").build(),
                AssistantMessageDto.builder().role("system").content("system msg").build(),
                AssistantMessageDto.builder().role("assistant").content("response").build(),
                AssistantMessageDto.builder().role("tool").content("tool result").build());

        PromptPayload payload = builder.build("system", history, "question");

        assertEquals(2, payload.history().size());
        assertTrue(payload.history().stream().allMatch(
                m -> "user".equals(m.getRole()) || "assistant".equals(m.getRole())));
    }

    @Test
    void build_normalizesWhitespace() {
        List<AssistantMessageDto> history = List.of(
                AssistantMessageDto.builder().role("  user  ").content("  hello  ").build());

        PromptPayload payload = builder.build("  system prompt  ", history, "  question  ");

        assertEquals("system prompt", payload.systemPrompt());
        assertEquals("user", payload.history().get(0).getRole());
        assertEquals("hello", payload.history().get(0).getContent());
        assertEquals("question", payload.userMessage());
    }

    @Test
    void build_handlesNullHistory() {
        PromptPayload payload = builder.build("system", null, "question");

        assertNotNull(payload.history());
        assertTrue(payload.history().isEmpty());
    }

    @Test
    void build_handlesNullFieldsInMessages() {
        List<AssistantMessageDto> history = List.of(
                AssistantMessageDto.builder().role(null).content("valid").build());

        PromptPayload payload = builder.build(null, history, null);

        assertEquals("", payload.systemPrompt());
        assertEquals("", payload.userMessage());
        assertTrue(payload.history().isEmpty());
    }
}
