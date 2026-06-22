package com.saveapenny.assistant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.assistant.dto.AssistantChatRequest;
import com.saveapenny.assistant.dto.AssistantChatResponse;
import com.saveapenny.assistant.exception.AssistantDisabledException;
import com.saveapenny.assistant.service.AssistantService;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AssistantController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssistantService assistantService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUpRateLimitingFilter() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
    }

    @Test
    void chat_returnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("How can I save more this month?")
                .build();

        AssistantChatResponse response = AssistantChatResponse.builder()
                .sessionId(UUID.randomUUID())
                .reply("Start by reducing variable food spending.")
                .disclaimer("This assistant provides general budgeting guidance, not financial, tax, or legal advice.")
                .build();

        when(jwtService.isAccessTokenValid("token-a1")).thenReturn(true);
        when(jwtService.extractUserId("token-a1")).thenReturn(userId);
        when(assistantService.chat(eq(userId), any(AssistantChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer token-a1")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(response.getSessionId().toString()))
                .andExpect(jsonPath("$.data.reply").value("Start by reducing variable food spending."))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void chat_returnsBadRequest_whenMessageIsBlank() throws Exception {
        AssistantChatRequest request = AssistantChatRequest.builder()
                .message(" ")
                .build();

        when(jwtService.isAccessTokenValid("token-a2")).thenReturn(true);
        when(jwtService.extractUserId("token-a2")).thenReturn(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer token-a2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void chat_returnsBadRequest_whenHistoryContentIsBlank() throws Exception {
        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("How can I save more?")
                .history(java.util.List.of(com.saveapenny.assistant.dto.AssistantMessageDto.builder()
                        .role("user")
                        .content(" ")
                        .build()))
                .build();

        when(jwtService.isAccessTokenValid("token-a4")).thenReturn(true);
        when(jwtService.extractUserId("token-a4")).thenReturn(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer token-a4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0]").value("history[0].content: must not be blank"));
    }

    @Test
    void chat_returnsUnauthorized_whenAuthContextMissing() throws Exception {
        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("How can I save more?")
                .build();

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void chat_returnsServiceUnavailable_whenAssistantDisabled() throws Exception {
        UUID userId = UUID.randomUUID();
        AssistantChatRequest request = AssistantChatRequest.builder()
                .message("How can I save more?")
                .build();

        when(jwtService.isAccessTokenValid("token-a3")).thenReturn(true);
        when(jwtService.extractUserId("token-a3")).thenReturn(userId);
        when(assistantService.chat(eq(userId), any(AssistantChatRequest.class)))
                .thenThrow(new AssistantDisabledException());

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer token-a3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ASSISTANT_DISABLED"));
    }
}
