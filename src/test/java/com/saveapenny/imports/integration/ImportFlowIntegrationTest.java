package com.saveapenny.imports.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:import-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class ImportFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUpRole() {
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
    }

    @Test
    void previewConfirmAndStatusFlow_worksEndToEnd() throws Exception {
        String token = registerAndGetToken("imports.flow@example.com", "Imports Flow");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                ("type,date,amount,currency,accountId,categoryId\n"
                        + "EXPENSE,2026-05-01,10.50,USD,a1,c1\n"
                        + "EXPENSE,2026-05-02,invalid,USD,a2,c2\n")
                        .getBytes());

        MvcResult previewResult = mockMvc.perform(multipart("/api/v1/imports/transactions/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalRows").value(2))
                .andExpect(jsonPath("$.data.invalidRows").value(1))
                .andReturn();

        String importId = extractField(previewResult, "data", "importId");

        String confirmBody = "{\"importId\":\"" + importId + "\"}";
        MvcResult confirmResult = mockMvc.perform(post("/api/v1/imports/transactions/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isOk())
                .andReturn();

        String initialStatus = extractField(confirmResult, "data", "status");
        assertTrue("RUNNING".equals(initialStatus) || "COMPLETED".equals(initialStatus));

        String finalStatus = initialStatus;
        for (int i = 0; i < 30 && !"COMPLETED".equals(finalStatus) && !"FAILED".equals(finalStatus); i++) {
            Thread.sleep(100);
            MvcResult statusResult = mockMvc.perform(get("/api/v1/imports/transactions/{importId}/status", importId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
            finalStatus = extractField(statusResult, "data", "status");
        }

        mockMvc.perform(get("/api/v1/imports/transactions/{importId}/status", importId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(finalStatus))
                .andExpect(jsonPath("$.data.totalRows").value(2));

        assertTrue("COMPLETED".equals(finalStatus) || "FAILED".equals(finalStatus));
    }

    @Test
    void preview_returnsBadRequest_forInvalidFile() throws Exception {
        String token = registerAndGetToken("imports.invalid@example.com", "Imports Invalid");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.txt",
                "text/plain",
                "not-a-csv".getBytes());

        mockMvc.perform(multipart("/api/v1/imports/transactions/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_IMPORT_FILE"));
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
}
