package com.saveapenny.transaction.integration;

import com.saveapenny.test.TestcontainersIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:transaction-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class TransactionFlowIntegrationTest extends TestcontainersIntegrationTest {

    @Test
    void transactionAndTransferFlow_worksForAuthenticatedUser() throws Exception {
        String token = register("transaction.flow@example.com", "Transaction Flow");

        String fromAccountBody = """
                {"name":"Cash","type":"CASH","currency":"USD","initialBalance":1000.0000}
                """;
        String toAccountBody = """
                {"name":"Bank","type":"BANK","currency":"USD","initialBalance":200.0000}
                """;

        String fromAccountId = extractId(mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fromAccountBody))
                .andExpect(status().isCreated())
                .andReturn());

        String toAccountId = extractId(mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toAccountBody))
                .andExpect(status().isCreated())
                .andReturn());

        String categoryBody = """
                {"name":"Food","type":"EXPENSE","color":"#ff0000","icon":"utensils"}
                """;
        String categoryId = extractId(mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody))
                .andExpect(status().isCreated())
                .andReturn());

        String createTransactionBody = """
                {
                  "accountId":"%s",
                  "categoryId":"%s",
                  "type":"EXPENSE",
                  "amount":120.0000,
                  "currency":"USD",
                  "description":"Groceries",
                  "transactionDate":"2026-05-12"
                }
                """.formatted(fromAccountId, categoryId);

        String transactionId = extractId(mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTransactionBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("EXPENSE"))
                .andReturn());

        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").exists());

        String updateBody = """
                {
                  "accountId":"%s",
                  "categoryId":"%s",
                  "type":"EXPENSE",
                  "amount":100.0000,
                  "currency":"USD",
                  "description":"Updated groceries",
                  "transactionDate":"2026-05-12"
                }
                """.formatted(fromAccountId, categoryId);

        mockMvc.perform(put("/api/v1/transactions/{id}", transactionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(100.0));

        String transferBody = """
                {
                  "fromAccountId":"%s",
                  "toAccountId":"%s",
                  "categoryId":"%s",
                  "amount":50.0000,
                  "currency":"USD",
                  "description":"Move funds",
                  "transactionDate":"2026-05-12"
                }
                """.formatted(fromAccountId, toAccountId, categoryId);

        String transferTransactionId = extractField(mockMvc.perform(post("/api/v1/transactions/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody))
                .andExpect(status().isCreated())
                .andReturn(), "transactionId");

        mockMvc.perform(delete("/api/v1/transactions/{id}", transferTransactionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String invalidCurrencyTransactionBody = """
                {
                  "accountId":"%s",
                  "categoryId":"%s",
                  "type":"EXPENSE",
                  "amount":25.0000,
                  "currency":"EUR",
                  "description":"Wrong currency",
                  "transactionDate":"2026-05-13"
                }
                """.formatted(fromAccountId, categoryId);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCurrencyTransactionBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_TRANSACTION_CURRENCY"));
    }

}
