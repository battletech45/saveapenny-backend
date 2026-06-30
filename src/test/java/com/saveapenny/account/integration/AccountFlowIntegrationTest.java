package com.saveapenny.account.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.test.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:account-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class AccountFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void accountCrudFlow_worksForAuthenticatedUser() throws Exception {
        String accessToken = register("account.flow@example.com", "Account Flow");

        String createBody = """
                {
                  "name": "Wallet",
                  "type": "CASH",
                  "currency": "USD",
                  "initialBalance": 250.0000
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Wallet"))
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String accountId = createJson.path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].name").value("Wallet"));

        String updateBody = """
                {
                  "name": "Main Wallet",
                  "type": "CASH",
                  "currency": "USD"
                }
                """;

        mockMvc.perform(put("/api/v1/accounts/{id}", accountId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Main Wallet"))
                .andExpect(jsonPath("$.data.currency").value("USD"));

        mockMvc.perform(delete("/api/v1/accounts/{id}", accountId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String recreateReservedNameBody = """
                {
                  "name": "Main Wallet",
                  "type": "CASH",
                  "currency": "USD",
                  "initialBalance": 250.0000
                }
                """;

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recreateReservedNameBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NAME_ALREADY_EXISTS"));

        mockMvc.perform(get("/api/v1/accounts/{id}", accountId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"));
    }
}
