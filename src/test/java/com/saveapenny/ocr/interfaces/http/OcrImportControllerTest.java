package com.saveapenny.ocr.interfaces.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import com.saveapenny.ocr.application.port.in.OcrJobService;
import com.saveapenny.ocr.domain.model.OcrJobStatus;
import com.saveapenny.ocr.interfaces.http.dto.OcrJobStatusResponse;
import com.saveapenny.ocr.interfaces.http.dto.OcrSubmitResponse;
import jakarta.servlet.FilterChain;
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

@WebMvcTest(OcrImportController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class OcrImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OcrJobService ocrJobService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    private final UUID userId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
    }

    @Test
    void upload_returns202() throws Exception {
        when(jwtService.isAccessTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn(userId);

        OcrSubmitResponse response = OcrSubmitResponse.builder()
                .jobId(jobId)
                .status(OcrJobStatus.PENDING)
                .build();
        when(ocrJobService.createJob(any(), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.png", "image/png", "fake-image-data".getBytes());

        mockMvc.perform(multipart("/api/imports/ocr")
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void upload_withoutAuth_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.png", "image/png", "data".getBytes());

        mockMvc.perform(multipart("/api/imports/ocr")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStatus_returns200() throws Exception {
        when(jwtService.isAccessTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn(userId);

        OcrJobStatusResponse statusResponse = OcrJobStatusResponse.builder()
                .jobId(jobId)
                .status(OcrJobStatus.COMPLETED)
                .originalFileName("receipt.png")
                .build();
        when(ocrJobService.getJobStatus(userId, jobId)).thenReturn(statusResponse);

        mockMvc.perform(get("/api/imports/ocr/{jobId}", jobId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.originalFileName").value("receipt.png"));
    }

    @Test
    void getStatus_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/imports/ocr/{jobId}", jobId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStatus_whenJobNotFound_returns404() throws Exception {
        when(jwtService.isAccessTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn(userId);

        when(ocrJobService.getJobStatus(userId, jobId))
                .thenThrow(new com.saveapenny.ocr.domain.exception.OcrJobNotFoundException(jobId));

        mockMvc.perform(get("/api/imports/ocr/{jobId}", jobId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("OCR_JOB_NOT_FOUND"));
    }
}
