package com.saveapenny.imports.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import com.saveapenny.imports.dto.ImportPreviewResponse;
import com.saveapenny.imports.dto.ImportStatusResponse;
import com.saveapenny.imports.entity.ImportStatus;
import com.saveapenny.imports.exception.ImportNotFoundException;
import com.saveapenny.imports.service.ImportService;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImportController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ImportService importService;

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
    void preview_returnsCreatedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID importId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-i1")).thenReturn(true);
        when(jwtService.extractUserId("token-i1")).thenReturn(userId);
        when(importService.preview(eq(userId), any())).thenReturn(ImportPreviewResponse.builder()
                .importId(importId)
                .fileName("transactions.csv")
                .totalRows(2)
                .validRows(1)
                .invalidRows(1)
                .errors(List.of())
                .build());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                "type,date,amount,currency,accountId,categoryId\nEXPENSE,2026-05-01,12.50,USD,a1,c1\n".getBytes());

        mockMvc.perform(multipart("/api/v1/imports/transactions/preview")
                        .file(file)
                        .header("Authorization", "Bearer token-i1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.importId").value(importId.toString()));
    }

    @Test
    void confirm_returnsEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID importId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-i2")).thenReturn(true);
        when(jwtService.extractUserId("token-i2")).thenReturn(userId);
        when(importService.confirm(userId, importId)).thenReturn(ImportStatusResponse.builder()
                .importId(importId)
                .status(ImportStatus.RUNNING)
                .totalRows(1)
                .importedRows(0)
                .failedRows(0)
                .build());

        String body = objectMapper.writeValueAsString(java.util.Map.of("importId", importId));

        mockMvc.perform(post("/api/v1/imports/transactions/confirm")
                        .header("Authorization", "Bearer token-i2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void getStatus_returnsNotFound_whenMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID importId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-i3")).thenReturn(true);
        when(jwtService.extractUserId("token-i3")).thenReturn(userId);
        when(importService.getStatus(userId, importId)).thenThrow(new ImportNotFoundException(importId));

        mockMvc.perform(get("/api/v1/imports/transactions/{importId}/status", importId)
                        .header("Authorization", "Bearer token-i3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("IMPORT_NOT_FOUND"));
    }
}
