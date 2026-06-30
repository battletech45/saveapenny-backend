package com.saveapenny.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.audit.dto.AuditLogResponse;
import com.saveapenny.audit.entity.AuditAction;
import com.saveapenny.audit.entity.AuditEntityType;
import com.saveapenny.audit.exception.AuditLogNotFoundException;
import com.saveapenny.audit.exception.InvalidAuditDateRangeException;
import com.saveapenny.audit.service.AuditLogService;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
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

@WebMvcTest(AuditLogController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuditLogService auditLogService;

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
        UUID logId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-a1")).thenReturn(true);
        when(jwtService.extractUserId("token-a1")).thenReturn(userId);
        when(auditLogService.create(eq(userId), any())).thenReturn(AuditLogResponse.builder()
                .id(logId)
                .userId(userId)
                .action(AuditAction.CREATE)
                .entityType(AuditEntityType.ACCOUNT)
                .entityId(UUID.randomUUID())
                .createdAt(OffsetDateTime.now())
                .build());

        String body = """
                {
                  "action": "CREATE",
                  "entityType": "ACCOUNT",
                  "entityId": "%s",
                  "newValue": "Wallet created"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/audits")
                        .header("Authorization", "Bearer token-a1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(logId.toString()));
    }

    @Test
    void getAll_returnsPagedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-a2")).thenReturn(true);
        when(jwtService.extractUserId("token-a2")).thenReturn(userId);
        when(auditLogService.getAll(eq(userId), any(), any()))
                .thenReturn(new PageImpl<>(List.of(AuditLogResponse.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .action(AuditAction.UPDATE)
                        .entityType(AuditEntityType.BUDGET)
                        .entityId(UUID.randomUUID())
                        .createdAt(OffsetDateTime.now())
                        .build())));

        mockMvc.perform(get("/api/v1/audits")
                        .header("Authorization", "Bearer token-a2")
                        .param("entityType", "BUDGET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].entityType").value("BUDGET"));
    }

    @Test
    void getById_returnsNotFound_whenMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID logId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-a3")).thenReturn(true);
        when(jwtService.extractUserId("token-a3")).thenReturn(userId);
        when(auditLogService.getById(userId, logId)).thenThrow(new AuditLogNotFoundException(logId));

        mockMvc.perform(get("/api/v1/audits/{auditLogId}", logId)
                        .header("Authorization", "Bearer token-a3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUDIT_LOG_NOT_FOUND"));
    }

    @Test
    void unauthenticatedRequest_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/audits"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void getAll_returnsBadRequest_whenDateRangeInvalid() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-a4")).thenReturn(true);
        when(jwtService.extractUserId("token-a4")).thenReturn(userId);
        when(auditLogService.getAll(eq(userId), any(), any()))
                .thenThrow(new InvalidAuditDateRangeException(
                        OffsetDateTime.parse("2026-05-15T00:00:00Z"),
                        OffsetDateTime.parse("2026-05-01T00:00:00Z")));

        mockMvc.perform(get("/api/v1/audits")
                        .header("Authorization", "Bearer token-a4")
                        .param("from", "2026-05-15T00:00:00Z")
                        .param("to", "2026-05-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_AUDIT_DATE_RANGE"));
    }
}
