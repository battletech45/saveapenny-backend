package com.saveapenny.report.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.user.entity.Role;
import com.saveapenny.user.repository.RoleRepository;
import java.time.LocalDate;
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
        "spring.datasource.url=jdbc:h2:mem:report-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class ReportFlowIntegrationTest {

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
    void reportsEndpoints_returnExpectedAggregates_andRespectOwnership() throws Exception {
        String tokenA = registerAndGetToken("report.user.a@example.com", "Report User A");
        String tokenB = registerAndGetToken("report.user.b@example.com", "Report User B");

        String accountA = createAccount(tokenA, "Wallet A", "CASH", "USD", "1000.0000");
        createAccount(tokenA, "Credit A", "CREDIT", "USD", "300.0000");
        String categoryA = createCategory(tokenA, "Food A");

        createTransaction(tokenA, accountA, categoryA, "INCOME", "500.0000", "2026-05-10", "Salary");
        createTransaction(tokenA, accountA, categoryA, "EXPENSE", "120.0000", "2026-05-12", "Groceries");
        createTransaction(tokenA, accountA, categoryA, "EXPENSE", "80.0000", "2026-05-13", "Cafe");

        String accountB = createAccount(tokenB, "Wallet B", "CASH", "USD", "200.0000");
        String categoryB = createCategory(tokenB, "Food B");
        createTransaction(tokenB, accountB, categoryB, "INCOME", "999.0000", "2026-05-11", "Other salary");

        mockMvc.perform(get("/api/v1/reports/monthly-summary")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalIncome").value(500.0))
                .andExpect(jsonPath("$.data.totalExpense").value(200.0))
                .andExpect(jsonPath("$.data.netSavings").value(300.0));

        mockMvc.perform(get("/api/v1/reports/category-spending")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].categoryName").value("Food A"))
                .andExpect(jsonPath("$.data[0].totalAmount").value(200.0))
                .andExpect(jsonPath("$.data[0].usagePercentage").value(100.0));

        mockMvc.perform(get("/api/v1/reports/cash-flow")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].date").value("2026-05-10"))
                .andExpect(jsonPath("$.data[0].incomeAmount").value(500.0))
                .andExpect(jsonPath("$.data[0].netAmount").value(500.0));

        String snapshotDate = LocalDate.now().minusDays(1).toString();
        mockMvc.perform(get("/api/v1/reports/net-worth")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("snapshotDate", snapshotDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAssets").value(1300.0))
                .andExpect(jsonPath("$.data.totalLiabilities").value(300.0))
                .andExpect(jsonPath("$.data.netWorth").value(1000.0));

        mockMvc.perform(get("/api/v1/reports/monthly-summary")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("from", "2026-05-31")
                        .param("to", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REPORT_DATE_RANGE"));
    }

    private String registerAndGetToken(String email, String fullName) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "strong-pass-123",
                  "fullName": "%s"
                }
                """.formatted(email, fullName);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    private String createAccount(String token, String name, String type, String currency, String initialBalance) throws Exception {
        String body = """
                {
                  "name": "%s",
                  "type": "%s",
                  "currency": "%s",
                  "initialBalance": %s
                }
                """.formatted(name, type, currency, initialBalance);

        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asText();
    }

    private String createCategory(String token, String name) throws Exception {
        String body = """
                {
                  "name": "%s",
                  "type": "EXPENSE",
                  "color": "#ff0000",
                  "icon": "utensils"
                }
                """.formatted(name);

        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asText();
    }

    private void createTransaction(
            String token,
            String accountId,
            String categoryId,
            String type,
            String amount,
            String transactionDate,
            String description) throws Exception {
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
                """.formatted(accountId, categoryId, type, amount, description, transactionDate);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
