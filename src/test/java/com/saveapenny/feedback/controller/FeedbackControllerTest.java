package com.saveapenny.feedback.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import com.saveapenny.feedback.dto.CreateFeedbackRequest;
import com.saveapenny.feedback.dto.FeedbackResponse;
import com.saveapenny.feedback.entity.FeedbackType;
import com.saveapenny.feedback.exception.FeedbackNotFoundException;
import com.saveapenny.feedback.service.FeedbackService;
import jakarta.servlet.FilterChain;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FeedbackController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackService feedbackService;

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
    void create_returnsCreatedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-f1")).thenReturn(true);
        when(jwtService.extractUserId("token-f1")).thenReturn(userId);
        when(feedbackService.create(eq(userId), any(CreateFeedbackRequest.class))).thenReturn(FeedbackResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(FeedbackType.FEATURE_REQUEST)
                .rating(5)
                .message("Please add more export formats")
                .metadata(JsonNodeFactory.instance.objectNode().put("platform", "android"))
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build());

        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .type(FeedbackType.FEATURE_REQUEST)
                .rating(5)
                .message("Please add more export formats")
                .metadata(JsonNodeFactory.instance.objectNode().put("platform", "android"))
                .build();

        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", "Bearer token-f1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("FEATURE_REQUEST"))
                .andExpect(jsonPath("$.data.metadata.platform").value("android"));
    }

    @Test
    void create_returnsValidationFailed_whenMessageBlank() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-f2")).thenReturn(true);
        when(jwtService.extractUserId("token-f2")).thenReturn(userId);

        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .type(FeedbackType.GENERAL)
                .rating(4)
                .message(" ")
                .build();

        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", "Bearer token-f2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getAll_returnsPagedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-f3")).thenReturn(true);
        when(jwtService.extractUserId("token-f3")).thenReturn(userId);
        when(feedbackService.getAll(eq(userId), eq(FeedbackType.BUG_REPORT), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/feedback")
                        .param("type", "BUG_REPORT")
                        .header("Authorization", "Bearer token-f3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].message").value("App freezes when opening reports"));
    }

    @Test
    void getById_returnsNotFound_whenMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID feedbackId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-f4")).thenReturn(true);
        when(jwtService.extractUserId("token-f4")).thenReturn(userId);
        when(feedbackService.getById(userId, feedbackId)).thenThrow(new FeedbackNotFoundException(feedbackId));

        mockMvc.perform(get("/api/v1/feedback/{feedbackId}", feedbackId)
                        .header("Authorization", "Bearer token-f4"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FEEDBACK_NOT_FOUND"));
    }

    @Test
    void delete_returnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID feedbackId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-f5")).thenReturn(true);
        when(jwtService.extractUserId("token-f5")).thenReturn(userId);
        doNothing().when(feedbackService).delete(userId, feedbackId);

        mockMvc.perform(delete("/api/v1/feedback/{feedbackId}", feedbackId)
                        .header("Authorization", "Bearer token-f5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unauthenticatedRequest_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/feedback"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    private FeedbackResponse sampleResponse() {
        return FeedbackResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .type(FeedbackType.BUG_REPORT)
                .rating(2)
                .message("App freezes when opening reports")
                .metadata(JsonNodeFactory.instance.objectNode().put("platform", "android"))
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
