package com.saveapenny.insight.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import com.saveapenny.insight.dto.InsightResponse;
import com.saveapenny.insight.entity.InsightType;
import com.saveapenny.insight.exception.InsightNotFoundException;
import com.saveapenny.insight.service.InsightService;
import com.saveapenny.shared.api.PagedResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InsightController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class InsightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InsightService insightService;

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
    void getAll_returnsPaginatedInsights() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "insight-token";
        when(jwtService.isAccessTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);

        InsightResponse insight = InsightResponse.builder()
                .id(UUID.randomUUID())
                .type(InsightType.SPENDING_PATTERN)
                .title("Test insight")
                .summary("Test summary")
                .severity("INFO")
                .generatedAt(OffsetDateTime.now())
                .build();

        PagedResponse<InsightResponse> listResponse = new PagedResponse<>(
                List.of(insight),
                0,
                20,
                1,
                1,
                false,
                false);

        when(insightService.getAll(eq(userId), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(listResponse);

        mockMvc.perform(get("/api/v1/insights")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.insights").isArray())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getById_returnsInsight() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID insightId = UUID.randomUUID();
        String token = "insight-token-2";
        when(jwtService.isAccessTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);

        InsightResponse insight = InsightResponse.builder()
                .id(insightId)
                .type(InsightType.ANOMALY)
                .title("Anomaly detected")
                .summary("Unusual transaction")
                .severity("WARNING")
                .generatedAt(OffsetDateTime.now())
                .build();

        when(insightService.getById(userId, insightId)).thenReturn(insight);

        mockMvc.perform(get("/api/v1/insights/{id}", insightId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(insightId.toString()));
    }

    @Test
    void markAsRead_returnsUpdatedInsight() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID insightId = UUID.randomUUID();
        String token = "insight-token-3";
        when(jwtService.isAccessTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);

        InsightResponse insight = InsightResponse.builder()
                .id(insightId)
                .type(InsightType.SPENDING_PATTERN)
                .title("Test")
                .summary("Summary")
                .severity("INFO")
                .read(true)
                .generatedAt(OffsetDateTime.now())
                .build();

        when(insightService.markAsRead(userId, insightId)).thenReturn(insight);

        mockMvc.perform(patch("/api/v1/insights/{id}/read", insightId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true))
                .andExpect(jsonPath("$.data.isRead").value(true));
    }

    @Test
    void dismiss_returnsDismissedInsight() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID insightId = UUID.randomUUID();
        String token = "insight-token-4";
        when(jwtService.isAccessTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);

        InsightResponse insight = InsightResponse.builder()
                .id(insightId)
                .type(InsightType.SPENDING_PATTERN)
                .title("Test")
                .summary("Summary")
                .severity("INFO")
                .dismissed(true)
                .generatedAt(OffsetDateTime.now())
                .build();

        when(insightService.dismiss(userId, insightId)).thenReturn(insight);

        mockMvc.perform(patch("/api/v1/insights/{id}/dismiss", insightId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dismissed").value(true));
    }

    @Test
    void generate_triggersGeneration() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "insight-token-5";
        when(jwtService.isAccessTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(insightService.generate(eq(userId), eq(null))).thenReturn(3);

        mockMvc.perform(post("/api/v1/insights/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(3));
    }

    @Test
    void getById_returnsNotFound_whenMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID insightId = UUID.randomUUID();
        String token = "insight-token-err-1";
        when(jwtService.isAccessTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(insightService.getById(userId, insightId)).thenThrow(new InsightNotFoundException(insightId));

        mockMvc.perform(get("/api/v1/insights/{id}", insightId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INSIGHT_NOT_FOUND"));
    }

    @Test
    void getAll_returnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/insights"))
                .andExpect(status().isUnauthorized());
    }
}
