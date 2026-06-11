package com.saveapenny.automation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.automation.repository.RecurringExecutionHistoryRepository;
import com.saveapenny.automation.repository.RecurringTransactionRepository;
import com.saveapenny.automation.service.RecurringTransactionExecutionService;
import com.saveapenny.transaction.repository.TransactionRepository;
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
        "spring.datasource.url=jdbc:h2:mem:recurring-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "automation.recurring.cron=-"
})
class RecurringTransactionFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RecurringTransactionExecutionService executionService;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RecurringExecutionHistoryRepository executionHistoryRepository;

    private String today;
    private String tomorrow;

    @BeforeEach
    void setUpDates() {
        today = LocalDate.now().toString();
        tomorrow = LocalDate.now().plusDays(1).toString();
    }

    @BeforeEach
    void setUpRole() {
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
    }

    @Test
    void recurringTransactionFullLifecycle_withExecution_works() throws Exception {
        String token = registerAndGetToken("recurring.flow@example.com", "Recurring Flow");

        String accountId = createAccount(token, "Cash", "CASH", "USD", "1000.0000");
        String categoryId = createCategory(token, "Salary", "INCOME", "#00ff00", "dollar");

        String createBody = """
                {
                  "accountId":"%s",
                  "categoryId":"%s",
                  "type":"INCOME",
                  "amount":50.0000,
                  "frequency":"DAILY",
                  "nextRunDate":"%s"
                }
                """.formatted(accountId, categoryId, today);

        String recurringId = extractId(mockMvc.perform(post("/api/v1/automations/recurring-transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nextRunDate").value(today))
                .andReturn());

        mockMvc.perform(get("/api/v1/automations/recurring-transactions/{id}", recurringId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(recurringId))
                .andExpect(jsonPath("$.data.amount").value(50.0))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/automations/recurring-transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1));

        executionService.processDueRecurringTransactions(LocalDate.parse(today));

        assertThat(transactionRepository.count()).isPositive();

        mockMvc.perform(get("/api/v1/automations/recurring-transactions/{id}", recurringId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextRunDate").value(tomorrow));

        String updateBody = """
                {
                  "accountId":"%s",
                  "categoryId":"%s",
                  "type":"INCOME",
                  "amount":75.0000,
                  "frequency":"DAILY",
                  "nextRunDate":"%s",
                  "status":"ACTIVE"
                }
                """.formatted(accountId, categoryId, tomorrow);

        mockMvc.perform(put("/api/v1/automations/recurring-transactions/{id}", recurringId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(75.0));

        mockMvc.perform(patch("/api/v1/automations/recurring-transactions/{id}/pause", recurringId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(patch("/api/v1/automations/recurring-transactions/{id}/resume", recurringId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/automations/recurring-transactions/{id}/history", recurringId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].scheduledDate").value(today))
                .andExpect(jsonPath("$.data.content[0].status").value("SUCCESS"));

        assertThat(executionHistoryRepository.findAll().stream()
                .filter(item -> item.getRecurringTransactionId().toString().equals(recurringId))
                .toList())
                .hasSize(1);
        assertThat(executionHistoryRepository.findAll().stream()
                .filter(item -> item.getRecurringTransactionId().toString().equals(recurringId))
                .findFirst()
                .orElseThrow()
                .getScheduledDate()).isEqualTo(LocalDate.parse(today));

        mockMvc.perform(get("/api/v1/automations/recurring-transactions/upcoming")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(delete("/api/v1/automations/recurring-transactions/{id}", recurringId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/automations/recurring-transactions/{id}", recurringId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RECURRING_TRANSACTION_NOT_FOUND"));
    }

    @Test
    void upcoming_returnsOnlyCurrentUsersRecurringTransactions() throws Exception {
        String firstToken = registerAndGetToken("recurring.upcoming.one@example.com", "Recurring Upcoming One");
        String secondToken = registerAndGetToken("recurring.upcoming.two@example.com", "Recurring Upcoming Two");

        String firstAccountId = createAccount(firstToken, "First Cash", "CASH", "USD", "1000.0000");
        String firstCategoryId = createCategory(firstToken, "First Salary", "INCOME", "#00ff00", "dollar");
        String secondAccountId = createAccount(secondToken, "Second Cash", "CASH", "USD", "1000.0000");
        String secondCategoryId = createCategory(secondToken, "Second Salary", "INCOME", "#0000ff", "banknote");

        String firstRecurringId = createRecurringTransaction(firstToken, firstAccountId, firstCategoryId, today);
        createRecurringTransaction(secondToken, secondAccountId, secondCategoryId, today);

        mockMvc.perform(get("/api/v1/automations/recurring-transactions/upcoming")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(10))
                .andExpect(jsonPath("$.data[0].recurringTransactionId").value(firstRecurringId));
    }

    @Test
    void upcoming_doesNotReturnDuplicateEntriesForSingleRecurringTransaction() throws Exception {
        String token = registerAndGetToken("recurring.upcoming.dupes@example.com", "Recurring Upcoming Dupes");
        String accountId = createAccount(token, "Dupes Cash", "CASH", "USD", "1000.0000");
        String categoryId = createCategory(token, "Dupes Income", "INCOME", "#ff00ff", "wallet");
        String recurringId = createRecurringTransaction(token, accountId, categoryId, today);

        mockMvc.perform(get("/api/v1/automations/recurring-transactions/upcoming?limit=3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].recurringTransactionId").value(recurringId))
                .andExpect(jsonPath("$.data[0].scheduledDate").value(today))
                .andExpect(jsonPath("$.data[1].scheduledDate").value(tomorrow))
                .andExpect(jsonPath("$.data[2].scheduledDate").value(LocalDate.parse(today).plusDays(2).toString()));
    }

    private String registerAndGetToken(String email, String fullName) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "Strong@123",
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

    private String createAccount(String token, String name, String type, String currency, String balance) throws Exception {
        String body = """
                {"name":"%s","type":"%s","currency":"%s","initialBalance":%s}
                """.formatted(name, type, currency, balance);

        return extractId(mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String createCategory(String token, String name, String type, String color, String icon) throws Exception {
        String body = """
                {"name":"%s","type":"%s","color":"%s","icon":"%s"}
                """.formatted(name, type, color, icon);

        return extractId(mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String createRecurringTransaction(String token, String accountId, String categoryId, String nextRunDate) throws Exception {
        String createBody = """
                {
                  "accountId":"%s",
                  "categoryId":"%s",
                  "type":"INCOME",
                  "amount":50.0000,
                  "frequency":"DAILY",
                  "nextRunDate":"%s"
                }
                """.formatted(accountId, categoryId, nextRunDate);

        return extractId(mockMvc.perform(post("/api/v1/automations/recurring-transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String extractId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asText();
    }
}
