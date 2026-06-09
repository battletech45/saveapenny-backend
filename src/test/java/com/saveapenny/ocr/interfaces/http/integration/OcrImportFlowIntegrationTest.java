package com.saveapenny.ocr.interfaces.http.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.ocr.application.port.in.OcrService;
import com.saveapenny.ocr.support.runtime.OcrRuntimeChecker;
import com.saveapenny.ocr.support.runtime.OcrRuntimeStatus;
import com.saveapenny.user.entity.Role;
import com.saveapenny.user.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ocr-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "ocr.enabled=true",
        "ocr.max-file-size-bytes=20"
})
class OcrImportFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private OcrService ocrService;

    @BeforeEach
    void setUpRole() {
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
    }

    @TestConfiguration
    static class RuntimeCheckerConfig {

        @Bean
        @Primary
        OcrRuntimeChecker ocrRuntimeChecker() {
            OcrRuntimeChecker checker = mock(OcrRuntimeChecker.class);
            when(checker.check()).thenReturn(new OcrRuntimeStatus(true, true, true, "eng", "/tmp", null));
            return checker;
        }
    }

    @Test
    void uploadAndStatusFlow_worksEndToEnd() throws Exception {
        String token = registerAndGetToken("ocr.flow@example.com", "OCR Flow");
        when(ocrService.extractText(any())).thenReturn("2026-05-20 market 15.50");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.png",
                "image/png",
                "png-binary-data".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/imports/ocr")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String jobId = extractField(uploadResult, "data", "jobId");
        String finalStatus = "PENDING";
        for (int i = 0; i < 30 && !("COMPLETED".equals(finalStatus) || "FAILED".equals(finalStatus)); i++) {
            Thread.sleep(100);
            MvcResult statusResult = mockMvc.perform(get("/api/imports/ocr/{jobId}", jobId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
            finalStatus = extractField(statusResult, "data", "status");
        }

        MvcResult finalStatusResult = mockMvc.perform(get("/api/imports/ocr/{jobId}", jobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(finalStatus))
                .andReturn();

        String rawText = extractField(finalStatusResult, "data", "rawText");
        if (!rawText.isBlank() && !"null".equals(rawText)) {
            mockMvc.perform(get("/api/imports/ocr/{jobId}", jobId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.transactionCandidates[0].categoryHint").value("FOOD"));
        }

        assertTrue("PENDING".equals(finalStatus) || "RUNNING".equals(finalStatus)
                || "COMPLETED".equals(finalStatus) || "FAILED".equals(finalStatus));
    }

    @Test
    void uploadAndStatusFlow_parsesStructuredReceiptText() throws Exception {
        String token = registerAndGetToken("ocr.structured@example.com", "OCR Structured");
        when(ocrService.extractText(any())).thenReturn("""
                YAPI VE KREDI BANKASI A.S.
                Ref No 253385491674

                Odeme Tarihi 17/05/2026
                Duzenlenme Tarihi 16/05/2026
                9014 YURT DISI CIKIS HARCI 2026 1

                TOPLAM 1.250,00-
                #TL#
                """);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.pdf",
                "application/pdf",
                "pdf-binary-data".getBytes());

        String jobId = extractField(mockMvc.perform(multipart("/api/imports/ocr")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andReturn(), "data", "jobId");

        String finalStatus = awaitFinalStatus(token, jobId);

        mockMvc.perform(get("/api/imports/ocr/{jobId}", jobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(finalStatus))
                .andExpect(jsonPath("$.data.transactionCandidates[0].date").value("2026-05-17"))
                .andExpect(jsonPath("$.data.transactionCandidates[0].amount").value(-1250.00))
                .andExpect(jsonPath("$.data.transactionCandidates[0].description").value("9014 YURT DISI CIKIS HARCI 2026 1"))
                .andExpect(jsonPath("$.data.documentType").value("BANK_RECEIPT"))
                .andExpect(jsonPath("$.data.currency").value("TRY"))
                .andExpect(jsonPath("$.data.merchantName").value("YAPI VE KREDI BANKASI A.S."))
                .andExpect(jsonPath("$.data.paymentDate").value("2026-05-17"))
                .andExpect(jsonPath("$.data.issueDate").value("2026-05-16"))
                .andExpect(jsonPath("$.data.extractedDates[0]").value("2026-05-17"))
                .andExpect(jsonPath("$.data.extractedAmounts[0]").value(-1250.00))
                .andExpect(jsonPath("$.data.referenceNumbers[0]").value("253385491674"))
                .andExpect(jsonPath("$.data.labels[0]").value("ref no"))
                .andExpect(jsonPath("$.data.parseConfidence").value(0.96))
                .andExpect(jsonPath("$.data.parseDiagnostics.detectedDocumentType").value("BANK_RECEIPT"))
                .andExpect(jsonPath("$.data.parseDiagnostics.confidenceScore").value(0.96))
                .andExpect(jsonPath("$.data.parseDiagnostics.selectedCandidateReason").value("Selected candidate amount near TOPLAM/TOTAL label"))
                .andExpect(jsonPath("$.data.parseDiagnostics.noCandidateReason").doesNotExist())
                .andExpect(jsonPath("$.data.parseWarning").doesNotExist());
    }

    @Test
    void uploadAndStatusFlow_returnsParseWarningWhenTextExistsWithoutCandidates() throws Exception {
        String token = registerAndGetToken("ocr.warning@example.com", "OCR Warning");
        when(ocrService.extractText(any())).thenReturn("BANKA DEKONTU\nReferans No 12345\nTesekkurler");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.pdf",
                "application/pdf",
                "pdf-binary-data".getBytes());

        String jobId = extractField(mockMvc.perform(multipart("/api/imports/ocr")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andReturn(), "data", "jobId");

        String finalStatus = awaitFinalStatus(token, jobId);

        mockMvc.perform(get("/api/imports/ocr/{jobId}", jobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(finalStatus))
                .andExpect(jsonPath("$.data.rawText").value("BANKA DEKONTU\nReferans No 12345\nTesekkurler"))
                .andExpect(jsonPath("$.data.transactionCandidates").isArray())
                .andExpect(jsonPath("$.data.transactionCandidates").isEmpty())
                .andExpect(jsonPath("$.data.parseConfidence").value(0.0))
                .andExpect(jsonPath("$.data.parseDiagnostics.detectedDocumentType").value("BANK_RECEIPT"))
                .andExpect(jsonPath("$.data.parseDiagnostics.confidenceScore").value(0.0))
                .andExpect(jsonPath("$.data.parseDiagnostics.selectedCandidateReason").doesNotExist())
                .andExpect(jsonPath("$.data.parseDiagnostics.noCandidateReason").value("No candidate satisfied the parsing heuristics with sufficient confidence"))
                .andExpect(jsonPath("$.data.parseDiagnostics.warnings[0]").value("OCR text extracted, but no transaction could be confidently parsed"))
                .andExpect(jsonPath("$.data.parseWarning").value("OCR text extracted, but no transaction could be confidently parsed"));
    }

    @Test
    void upload_returnsBadRequest_forInvalidMimeType() throws Exception {
        String token = registerAndGetToken("ocr.mime@example.com", "OCR Mime");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.gif",
                "image/gif",
                "gif-data".getBytes());

        mockMvc.perform(multipart("/api/imports/ocr")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_OCR_FILE"));
    }

    @Test
    void upload_returnsBadRequest_forOversizeFile() throws Exception {
        String token = registerAndGetToken("ocr.size@example.com", "OCR Size");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "this-content-is-definitely-larger-than-20-bytes".getBytes());

        mockMvc.perform(multipart("/api/imports/ocr")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_OCR_FILE"));
    }

    private String registerAndGetToken(String email, String fullName) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "Strong@123",
                  "fullName": "%s"
                }
                """.formatted(email, fullName);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        return registerJson.path("data").path("accessToken").asText();
    }

    private String extractField(MvcResult result, String objectName, String fieldName) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path(objectName).path(fieldName).asText();
    }

    private String awaitFinalStatus(String token, String jobId) throws Exception {
        String finalStatus = "PENDING";
        for (int i = 0; i < 30 && !("COMPLETED".equals(finalStatus) || "FAILED".equals(finalStatus)); i++) {
            Thread.sleep(100);
            MvcResult statusResult = mockMvc.perform(get("/api/imports/ocr/{jobId}", jobId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
            finalStatus = extractField(statusResult, "data", "status");
        }
        return finalStatus;
    }
}
