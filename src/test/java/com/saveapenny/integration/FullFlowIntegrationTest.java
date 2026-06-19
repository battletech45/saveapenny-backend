package com.saveapenny.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.test.TestcontainersIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:full-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class FullFlowIntegrationTest extends TestcontainersIntegrationTest {

    @Test
    void fullFlow_userRegistrationToGoalSimulation_worksEndToEnd() throws Exception {
        // 1. Register
        String token = register("full.flow@example.com", "Full Flow");

        // 2. Create account
        String accountId = createAccount(token);

        // 3. Create categories
        String expenseCategoryId = createCategory(token, "Groceries", "EXPENSE");
        String incomeCategoryId = createCategory(token, "Salary", "INCOME");

        // 4. Create transactions
        createTransaction(token, accountId, incomeCategoryId, "INCOME", "5000.0000", "2026-06-01", "Monthly salary");
        createTransaction(token, accountId, expenseCategoryId, "EXPENSE", "1500.0000", "2026-06-05", "Rent");
        createTransaction(token, accountId, expenseCategoryId, "EXPENSE", "200.0000", "2026-06-10", "Groceries");

        // 5. Create budget
        String budgetId = createBudget(token, expenseCategoryId);

        // 6. Get budget status
        mockMvc.perform(get("/api/v1/budgets/{budgetId}/status", budgetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category").value("Groceries"));

        // 7. Get monthly report
        mockMvc.perform(get("/api/v1/reports/monthly-summary")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalIncome").value(5000.0))
                .andExpect(jsonPath("$.data.totalExpense").value(1700.0));

        // 8. Create goal
        String goalId = createGoal(token, accountId);

        mockMvc.perform(get("/api/v1/goals/{goalId}", goalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarios[0].name").value("Baseline"));

        // 9. Simulate draft goal
        String simulateDraftBody = """
                {
                  "type": "SAVINGS",
                  "title": "Vacation Fund",
                  "targetAmount": 5000.0000,
                  "currency": "USD",
                  "targetDate": "2027-12-31",
                  "inputs": {
                    "version": 1,
                    "type": "SAVINGS",
                    "values": {
                      "targetAmount": 5000.0000,
                      "currency": "USD",
                      "targetDate": "2027-12-31",
                      "monthlyContribution": 300.0000,
                      "expectedAnnualReturn": 5.0,
                      "startBalance": 0.0
                    }
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/goals/simulate/draft")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simulateDraftBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.draft").value(true))
                .andExpect(jsonPath("$.data.result").exists());

        // 10. Simulate the created goal
        mockMvc.perform(post("/api/v1/goals/{goalId}/simulate", goalId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private String createAccount(String token) throws Exception {
        String body = """
                {"name":"Checking","type":"BANK","currency":"USD","initialBalance":5000.0000}
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asText();
    }

    private String createCategory(String token, String name, String type) throws Exception {
        String body = """
                {"name":"%s","type":"%s","color":"#ff0000","icon":"tag"}
                """.formatted(name, type);

        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asText();
    }

    private void createTransaction(String token, String accountId, String categoryId,
                                   String type, String amount, String date, String description) throws Exception {
        String body = """
                {
                  "accountId": "%s",
                  "categoryId": "%s",
                  "type": "%s",
                  "amount": %s,
                  "currency": "USD",
                  "description": "%s",
                  "transactionDate": "%s"
                }
                """.formatted(accountId, categoryId, type, amount, description, date);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private String createBudget(String token, String categoryId) throws Exception {
        String body = """
                {
                  "categoryId": "%s",
                  "amount": 2000.0000,
                  "period": "MONTHLY",
                  "startDate": "2026-06-01",
                  "endDate": "2026-06-30"
                }
                """.formatted(categoryId);

        MvcResult result = mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asText();
    }

    private String createGoal(String token, String accountId) throws Exception {
        String body = """
                {
                  "type": "SAVINGS",
                  "title": "Emergency Fund",
                  "targetAmount": 10000.0000,
                  "currency": "USD",
                  "targetDate": "2030-01-01",
                  "linkedAccountId": "%s",
                  "inputs": {
                    "version": 1,
                    "type": "SAVINGS",
                    "values": {
                      "targetAmount": 10000.0000,
                      "currency": "USD",
                      "targetDate": "2030-01-01",
                      "monthlyContribution": 300.0000,
                      "expectedAnnualReturn": 5.0,
                      "startBalance": 0.0
                    }
                  }
                }
                """.formatted(accountId);

        MvcResult result = mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asText();
    }
}
