package com.saveapenny.imports.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.account.entity.Account;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.test.TestcontainersIntegrationTest;
import com.saveapenny.transaction.repository.TransactionRepository;
import org.springframework.test.context.TestPropertySource;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:import-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class ImportFlowIntegrationTest extends TestcontainersIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void previewConfirmAndStatusFlow_persistsTransactionsAndUpdatesBalances() throws Exception {
        String token = register("imports.flow@example.com", "Imports Flow");
        grantPlusEntitlement(token);

        String accountId = extractField(mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Import Cash","type":"CASH","currency":"USD","initialBalance":1000.0000}
                                """))
                .andExpect(status().isCreated())
                .andReturn(), "data", "id");

        String categoryId = extractField(mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Imported Food","type":"EXPENSE","color":"#00ff00","icon":"receipt"}
                                """))
                .andExpect(status().isCreated())
                .andReturn(), "data", "id");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                ("type,date,amount,currency,accountId,categoryId\n"
                        + "EXPENSE,2026-05-01,10.50,USD,%s,%s,Lunch\n"
                        + "EXPENSE,2026-05-01,10.50,USD,%s,%s,Lunch\n"
                        + "EXPENSE,2026-05-02,invalid,USD,%s,%s,Bad amount\n")
                        .formatted(accountId, categoryId, accountId, categoryId, accountId, categoryId)
                        .getBytes());

        MvcResult previewResult = mockMvc.perform(multipart("/api/v1/imports/transactions/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalRows").value(3))
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
                .andExpect(jsonPath("$.data.totalRows").value(3))
                .andExpect(jsonPath("$.data.importedRows").value(1))
                .andExpect(jsonPath("$.data.failedRows").value(1));

        assertTrue("COMPLETED".equals(finalStatus) || "FAILED".equals(finalStatus));

        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].description").value("Lunch"))
                .andExpect(jsonPath("$.data.items[0].amount").value(10.5));

        assertEquals(1, transactionRepository.count());
        Account account = accountRepository.findById(UUID.fromString(accountId)).orElseThrow();
        assertEquals(0, account.getBalance().compareTo(new BigDecimal("989.5000")));
    }

    @Test
    void preview_returnsBadRequest_forInvalidFile() throws Exception {
        String token = register("imports.invalid@example.com", "Imports Invalid");
        grantPlusEntitlement(token);

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

    private String extractField(MvcResult result, String objectName, String fieldName) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path(objectName).path(fieldName).asText();
    }
}
