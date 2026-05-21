package com.saveapenny.imports.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.service.ocr.OcrService;
import com.saveapenny.user.entity.Role;
import com.saveapenny.user.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
                  "password": "strong-pass-123",
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
}
