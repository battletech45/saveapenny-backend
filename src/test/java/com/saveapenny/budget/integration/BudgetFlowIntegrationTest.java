package com.saveapenny.budget.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:budget-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class BudgetFlowIntegrationTest {

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
    void budgetCrudAndStatusFlow_worksForAuthenticatedUser() throws Exception {
        String registerBody = """
                {
                  "email": "budget.flow@example.com",
                  "password": "Strong@123",
                  "fullName": "Budget Flow"
                }
                """;

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String token = registerJson.path("data").path("accessToken").asText();

        String accountBody = """
                {"name":"Cash","type":"CASH","currency":"USD","initialBalance":1000.0000}
                """;
        String accountId = extractId(mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountBody))
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

        String budgetCreateBody = """
                {
                  "categoryId":"%s",
                  "amount":400.0000,
                  "period":"MONTHLY",
                  "startDate":"2026-05-01",
                  "endDate":"2026-05-31"
                }
                """.formatted(categoryId);

        String budgetId = extractId(mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetCreateBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.period").value("MONTHLY"))
                .andReturn());

        String createTransactionBody = """
                {
                  "accountId":"%s",
                  "categoryId":"%s",
                  "type":"EXPENSE",
                  "amount":100.0000,
                  "currency":"USD",
                  "description":"Groceries",
                  "transactionDate":"2026-05-12"
                }
                """.formatted(accountId, categoryId);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTransactionBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/budgets")
                        .param("period", "MONTHLY")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(budgetId));

        mockMvc.perform(get("/api/v1/budgets/{budgetId}/status", budgetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("Food"))
                .andExpect(jsonPath("$.data.spentAmount").value(100.0))
                .andExpect(jsonPath("$.data.usagePercentage").value(25.0))
                .andExpect(jsonPath("$.data.status").value("ON_TRACK"));

        String budgetUpdateBody = """
                {
                  "categoryId":"%s",
                  "amount":120.0000,
                  "period":"MONTHLY",
                  "startDate":"2026-05-01",
                  "endDate":"2026-05-31"
                }
                """.formatted(categoryId);

        mockMvc.perform(put("/api/v1/budgets/{budgetId}", budgetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetUpdateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(120.0));

        mockMvc.perform(get("/api/v1/budgets/{budgetId}/status", budgetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.usagePercentage").value(83.33))
                .andExpect(jsonPath("$.data.status").value("WARNING"));

        mockMvc.perform(delete("/api/v1/budgets/{budgetId}", budgetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/budgets/{budgetId}", budgetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BUDGET_NOT_FOUND"));
    }

    private String extractId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("id").asText();
    }
}
