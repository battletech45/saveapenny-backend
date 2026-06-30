package com.saveapenny.insight.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.user.entity.Role;
import com.saveapenny.user.repository.RoleRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:insight-integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "insight.stddev-threshold=1.5",
        "insight.max-insights-per-generation=20",
        "insight.deduplication-window-days=1",
        "insight.ai-enhanced=false",
        "rate-limit.login.max-per-minute=1000",
        "rate-limit.api.max-per-minute=10000"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InsightIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeAll
    void setUpRole() {
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
    }

    @Nested
    @DisplayName("Insight generation with real data")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class GenerationTests {

        private String token;
        private String accountId;
        private String foodCategoryId;
        private String entertainmentCategoryId;

        @BeforeEach
        void setUp() throws Exception {
            String email = "gen." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            token = registerAndGetToken(email, "Gen User");
            accountId = createAccount(token, "Checking", "CASH", "USD", "100000");
            foodCategoryId = createCategory(token, "Food", "EXPENSE", "#ff0000", "utensils");
            entertainmentCategoryId = createCategory(token, "Entertainment", "EXPENSE", "#00ff00", "film");
            seedHistoricalFoodTransactions();
            seedAnomalousTransaction();
            seedEntertainmentBudgetAndTransactions();
        }

        @Test
        @DisplayName("Should generate insights from transactions and budgets across all analyzers")
        void generate_withRealData_producesInsights() throws Exception {
            MvcResult genResult = mockMvc.perform(post("/api/v1/insights/generate")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.generatedCount").isNumber())
                    .andReturn();

            JsonNode genJson = objectMapper.readTree(genResult.getResponse().getContentAsString());
            int generatedCount = genJson.path("data").path("generatedCount").asInt();

            org.assertj.core.api.Assertions.assertThat(generatedCount)
                    .as("Should generate at least 1 insight from the seeded data")
                    .isGreaterThan(0);

            MvcResult listResult = mockMvc.perform(get("/api/v1/insights")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.totalItems").value(generatedCount))
                    .andReturn();

            JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
            JsonNode insights = listJson.path("data").path("items");

            boolean hasSpendingPattern = false;
            boolean hasAnomaly = false;
            boolean hasTrend = false;
            boolean hasRecommendation = false;
            boolean hasWarning = false;
            boolean hasCritical = false;

            for (JsonNode insight : insights) {
                String type = insight.path("type").asText();
                String severity = insight.path("severity").asText();
                if ("SPENDING_PATTERN".equals(type)) hasSpendingPattern = true;
                if ("ANOMALY".equals(type)) hasAnomaly = true;
                if ("TREND".equals(type)) hasTrend = true;
                if ("RECOMMENDATION".equals(type)) hasRecommendation = true;
                if ("WARNING".equals(severity)) hasWarning = true;
                if ("CRITICAL".equals(severity)) hasCritical = true;
            }

            org.assertj.core.api.Assertions.assertThat(hasSpendingPattern)
                    .as("Spending pattern should be detected from May-to-June increase in Food")
                    .isTrue();
            org.assertj.core.api.Assertions.assertThat(hasAnomaly)
                    .as("Anomaly should be detected from the $50000 transaction in Food")
                    .isTrue();
            org.assertj.core.api.Assertions.assertThat(hasTrend)
                    .as("Trend should be detected from 3-month increasing Food spending")
                    .isTrue();
            org.assertj.core.api.Assertions.assertThat(hasRecommendation)
                    .as("Recommendation should be created from exceeded Entertainment budget")
                    .isTrue();
            org.assertj.core.api.Assertions.assertThat(hasWarning)
                    .as("Should have at least one WARNING severity insight")
                    .isTrue();
            org.assertj.core.api.Assertions.assertThat(hasCritical)
                    .as("Should have at least one CRITICAL severity insight from exceeded budget")
                    .isTrue();
        }

        @Test
        @DisplayName("Should have generated insights with valid non-empty fields")
        void generatedInsights_haveValidFields() throws Exception {
            mockMvc.perform(post("/api/v1/insights/generate")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            MvcResult listResult = mockMvc.perform(get("/api/v1/insights")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
            JsonNode insights = listJson.path("data").path("items");

            for (JsonNode insight : insights) {
                String id = insight.path("id").asText();
                org.assertj.core.api.Assertions.assertThat(id).isNotBlank();

                String type = insight.path("type").asText();
                org.assertj.core.api.Assertions.assertThat(type)
                        .isIn("SPENDING_PATTERN", "ANOMALY", "TREND", "RECOMMENDATION");

                String severity = insight.path("severity").asText();
                org.assertj.core.api.Assertions.assertThat(severity)
                        .isIn("INFO", "WARNING", "CRITICAL");

                String title = insight.path("title").asText();
                String summary = insight.path("summary").asText();
                org.assertj.core.api.Assertions.assertThat(title).isNotBlank();
                org.assertj.core.api.Assertions.assertThat(summary).isNotBlank();

                String generatedAt = insight.path("generatedAt").asText();
                org.assertj.core.api.Assertions.assertThat(generatedAt).isNotBlank();
            }
        }

        @Test
        @DisplayName("Should not create duplicates on repeated generation within dedup window")
        void repeatedGeneration_doesNotCreateDuplicates() throws Exception {
            mockMvc.perform(post("/api/v1/insights/generate")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            MvcResult firstList = mockMvc.perform(get("/api/v1/insights")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();

            int firstCount = objectMapper.readTree(firstList.getResponse().getContentAsString())
                    .path("data").path("totalItems").asInt();

            MvcResult genAgain = mockMvc.perform(post("/api/v1/insights/generate")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.generatedCount").isNumber())
                    .andReturn();

            int secondGenerated = objectMapper.readTree(genAgain.getResponse().getContentAsString())
                    .path("data").path("generatedCount").asInt();

            MvcResult secondList = mockMvc.perform(get("/api/v1/insights")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();

            int secondCount = objectMapper.readTree(secondList.getResponse().getContentAsString())
                    .path("data").path("totalItems").asInt();

            org.assertj.core.api.Assertions.assertThat(secondGenerated)
                    .as("Repeated generation should create 0 new insights due to dedup")
                    .isEqualTo(0);
            org.assertj.core.api.Assertions.assertThat(secondCount)
                    .as("Total insight count should remain the same after dedup")
                    .isEqualTo(firstCount);
        }

        @Test
        @DisplayName("Should generate category-specific insight for budget insight")
        void budgetInsight_hasBudgetRelatedContent() throws Exception {
            mockMvc.perform(post("/api/v1/insights/generate")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            MvcResult listResult = mockMvc.perform(get("/api/v1/insights")
                            .param("type", "RECOMMENDATION")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andReturn();

            JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
            int totalElements = listJson.path("data").path("totalItems").asInt();

            org.assertj.core.api.Assertions.assertThat(totalElements)
                    .as("Should have at least 1 RECOMMENDATION insight for budget")
                    .isGreaterThanOrEqualTo(1);

            for (JsonNode insight : listJson.path("data").path("items")) {
                String title = insight.path("title").asText().toLowerCase();
                boolean relatesToBudget = title.contains("budget");
                org.assertj.core.api.Assertions.assertThat(relatesToBudget)
                        .as("All RECOMMENDATION insights should relate to budget: " + title)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should generate 0 insights when no analyzers produce results")
        void generate_withoutTransactions_producesZero() throws Exception {
            String email = "nodata." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            String noDataToken = registerAndGetToken(email, "No Data User");

            mockMvc.perform(post("/api/v1/insights/generate")
                            .header("Authorization", "Bearer " + noDataToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.generatedCount").value(0));
        }

        private void seedHistoricalFoodTransactions() throws Exception {
            createTransaction(token, accountId, foodCategoryId, "EXPENSE", "100.00", "2026-04-05", "Weekly groceries");
            createTransaction(token, accountId, foodCategoryId, "EXPENSE", "100.00", "2026-04-15", "Weekly groceries");
            createTransaction(token, accountId, foodCategoryId, "EXPENSE", "150.00", "2026-05-05", "Weekly groceries");
            createTransaction(token, accountId, foodCategoryId, "EXPENSE", "150.00", "2026-05-15", "Weekly groceries");
            createTransaction(token, accountId, foodCategoryId, "EXPENSE", "150.00", "2026-05-25", "Weekly groceries");
            createTransaction(token, accountId, foodCategoryId, "EXPENSE", "100.00", "2026-06-02", "Weekly groceries");
            createTransaction(token, accountId, foodCategoryId, "EXPENSE", "100.00", "2026-06-05", "Weekly groceries");
            createTransaction(token, accountId, foodCategoryId, "EXPENSE", "100.00", "2026-06-07", "Weekly groceries");
        }

        private void seedAnomalousTransaction() throws Exception {
            createTransaction(token, accountId, foodCategoryId, "EXPENSE", "50000.00", "2026-06-08", "Large purchase");
        }

        private void seedEntertainmentBudgetAndTransactions() throws Exception {
            String budgetBody = """
                    {
                      "categoryId":"%s",
                      "amount":200.0000,
                      "period":"MONTHLY",
                      "startDate":"2026-06-01",
                      "endDate":"2026-06-30"
                    }
                    """.formatted(entertainmentCategoryId);

            mockMvc.perform(post("/api/v1/budgets")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(budgetBody))
                    .andExpect(status().isCreated());

            createTransaction(token, accountId, entertainmentCategoryId, "EXPENSE", "100.00", "2026-06-03",
                    "Movie tickets");
            createTransaction(token, accountId, entertainmentCategoryId, "EXPENSE", "100.00", "2026-06-06", "Concert");
            createTransaction(token, accountId, entertainmentCategoryId, "EXPENSE", "100.00", "2026-06-09",
                    "Streaming service");
        }
    }

    @Nested
    @DisplayName("CRUD operations on insights")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class CrudTests {

        private String token;
        private String insightId;

        @BeforeEach
        void setUp() throws Exception {
            String email = "crud." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            token = registerAndGetToken(email, "Crud User");
            String accountId = createAccount(token, "Checking", "CASH", "USD", "100000");
            String foodCategoryId = createCategory(token, "Food", "EXPENSE", "#ff0000", "utensils");
            String entertainmentCategoryId = createCategory(token, "Entertainment", "EXPENSE", "#00ff00", "film");
            seedTransactions(accountId, foodCategoryId);
            seedBudget(accountId, entertainmentCategoryId);

            mockMvc.perform(post("/api/v1/insights/generate")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            insightId = getFirstInsightId();
        }

        @Test
        @DisplayName("GET /api/v1/insights returns paginated insights list")
        void listInsights_returnsPaginatedResults() throws Exception {
            mockMvc.perform(get("/api/v1/insights")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.page").isNumber())
                    .andExpect(jsonPath("$.data.size").isNumber())
                    .andExpect(jsonPath("$.data.totalItems").isNumber())
                    .andExpect(jsonPath("$.data.totalPages").isNumber());
        }

        @Test
        @DisplayName("GET /api/v1/insights supports filtering by type")
        void listInsights_filtersByType() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/insights")
                            .param("type", "ANOMALY")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andReturn();

            JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
            for (JsonNode insight : json.path("data").path("items")) {
                org.assertj.core.api.Assertions.assertThat(insight.path("type").asText())
                        .isEqualTo("ANOMALY");
            }
        }

        @Test
        @DisplayName("GET /api/v1/insights supports filtering by severity")
        void listInsights_filtersBySeverity() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/insights")
                            .param("severity", "CRITICAL")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
            int total = json.path("data").path("totalItems").asInt();
            org.assertj.core.api.Assertions.assertThat(total)
                    .as("Should have at least 1 CRITICAL insight from exceeded budget")
                    .isGreaterThanOrEqualTo(1);

            for (JsonNode insight : json.path("data").path("items")) {
                org.assertj.core.api.Assertions.assertThat(insight.path("severity").asText())
                        .isEqualTo("CRITICAL");
            }
        }

        @Test
        @DisplayName("GET /api/v1/insights supports filtering by read status")
        void listInsights_filtersByReadStatus() throws Exception {
            MvcResult unreadResult = mockMvc.perform(get("/api/v1/insights")
                            .param("isRead", "false")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();

            int unreadCount = objectMapper.readTree(unreadResult.getResponse().getContentAsString())
                    .path("data").path("totalItems").asInt();

            org.assertj.core.api.Assertions.assertThat(unreadCount)
                    .as("All insights should start as unread")
                    .isGreaterThan(0);
        }

        @Test
        @DisplayName("GET /api/v1/insights supports pagination params")
        void listInsights_supportsPagination() throws Exception {
            mockMvc.perform(get("/api/v1/insights")
                            .param("page", "0")
                            .param("size", "2")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.size").value(2));
        }

        @Test
        @DisplayName("GET /api/v1/insights/{id} returns a single insight")
        void getInsightById_returnsInsight() throws Exception {
            mockMvc.perform(get("/api/v1/insights/{id}", insightId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(insightId))
                    .andExpect(jsonPath("$.data.type").isString())
                    .andExpect(jsonPath("$.data.severity").isString())
                    .andExpect(jsonPath("$.data.title").isString())
                    .andExpect(jsonPath("$.data.summary").isString())
                    .andExpect(jsonPath("$.data.read").value(false))
                    .andExpect(jsonPath("$.data.dismissed").value(false))
                    .andExpect(jsonPath("$.data.generatedAt").isString());
        }

        @Test
        @DisplayName("PATCH /api/v1/insights/{id}/read marks insight as read")
        void markAsRead_updatesReadStatus() throws Exception {
            mockMvc.perform(patch("/api/v1/insights/{id}/read", insightId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(insightId))
                    .andExpect(jsonPath("$.data.read").value(true));

            mockMvc.perform(get("/api/v1/insights/{id}", insightId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data.read").value(true));
        }

        @Test
        @DisplayName("PATCH /api/v1/insights/{id}/dismiss marks insight as dismissed")
        void dismiss_updatesDismissedStatus() throws Exception {
            mockMvc.perform(patch("/api/v1/insights/{id}/dismiss", insightId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(insightId))
                    .andExpect(jsonPath("$.data.dismissed").value(true));

            mockMvc.perform(get("/api/v1/insights/{id}", insightId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data.dismissed").value(true));
        }

        @Test
        @DisplayName("GET /api/v1/insights/{id} returns 404 for non-existent insight")
        void getInsightById_withInvalidId_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/insights/{id}", "00000000-0000-0000-0000-000000000000")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("INSIGHT_NOT_FOUND"));
        }

        @Test
        @DisplayName("PATCH /api/v1/insights/{id}/read returns 404 for non-existent insight")
        void markAsRead_withInvalidId_returns404() throws Exception {
            mockMvc.perform(patch("/api/v1/insights/{id}/read", "00000000-0000-0000-0000-000000000000")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("INSIGHT_NOT_FOUND"));
        }

        @Test
        @DisplayName("PATCH /api/v1/insights/{id}/dismiss returns 404 for non-existent insight")
        void dismiss_withInvalidId_returns404() throws Exception {
            mockMvc.perform(patch("/api/v1/insights/{id}/dismiss", "00000000-0000-0000-0000-000000000000")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("INSIGHT_NOT_FOUND"));
        }

        private void seedTransactions(String accountId, String categoryId) throws Exception {
            createTransaction(token, accountId, categoryId, "EXPENSE", "100.00", "2026-04-05", "Weekly groceries");
            createTransaction(token, accountId, categoryId, "EXPENSE", "100.00", "2026-04-15", "Weekly groceries");
            createTransaction(token, accountId, categoryId, "EXPENSE", "150.00", "2026-05-05", "Weekly groceries");
            createTransaction(token, accountId, categoryId, "EXPENSE", "150.00", "2026-05-15", "Weekly groceries");
            createTransaction(token, accountId, categoryId, "EXPENSE", "150.00", "2026-05-25", "Weekly groceries");
            createTransaction(token, accountId, categoryId, "EXPENSE", "100.00", "2026-06-02", "Weekly groceries");
            createTransaction(token, accountId, categoryId, "EXPENSE", "100.00", "2026-06-05", "Weekly groceries");
            createTransaction(token, accountId, categoryId, "EXPENSE", "100.00", "2026-06-07", "Weekly groceries");
            createTransaction(token, accountId, categoryId, "EXPENSE", "50000.00", "2026-06-08", "Large purchase");
        }

        private void seedBudget(String accountId, String categoryId) throws Exception {
            String budgetBody = """
                    {
                      "categoryId":"%s",
                      "amount":200.0000,
                      "period":"MONTHLY",
                      "startDate":"2026-06-01",
                      "endDate":"2026-06-30"
                    }
                    """.formatted(categoryId);

            mockMvc.perform(post("/api/v1/budgets")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(budgetBody))
                    .andExpect(status().isCreated());

            createTransaction(token, accountId, categoryId, "EXPENSE", "100.00", "2026-06-03", "Movie tickets");
            createTransaction(token, accountId, categoryId, "EXPENSE", "100.00", "2026-06-06", "Concert");
            createTransaction(token, accountId, categoryId, "EXPENSE", "100.00", "2026-06-09", "Streaming service");
        }

        private String getFirstInsightId() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/insights")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
            return json.path("data").path("items").get(0).path("id").asText();
        }
    }

    @Nested
    @DisplayName("Authorization and error handling")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class AuthTests {

        @Test
        @DisplayName("All insight endpoints reject unauthenticated requests")
        void insightEndpoints_rejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/insights"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/v1/insights/generate")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/v1/insights/{id}", "00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(patch("/api/v1/insights/{id}/read", "00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(patch("/api/v1/insights/{id}/dismiss", "00000000-0000-0000-0000-000000000000"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("User cannot access another user's insights")
        void user_cannotAccessOtherUsersInsights() throws Exception {
            String email1 = "user1." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            String token1 = registerAndGetToken(email1, "User One");
            String email2 = "user2." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            String token2 = registerAndGetToken(email2, "User Two");

            String accountId = createAccount(token1, "Checking", "CASH", "USD", "100000");
            String categoryId = createCategory(token1, "Food", "EXPENSE", "#ff0000", "utensils");
            createTransaction(token1, accountId, categoryId, "EXPENSE", "100.00", "2026-06-01", "Test");

            mockMvc.perform(post("/api/v1/insights/generate")
                            .header("Authorization", "Bearer " + token1)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/insights")
                            .header("Authorization", "Bearer " + token2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalItems").value(0));
        }
    }

    // ---- Shared helper methods ----

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

    private String createAccount(String token, String name, String type, String currency, String balance)
            throws Exception {
        String body = """
                {"name":"%s","type":"%s","currency":"%s","initialBalance":%s}
                """.formatted(name, type, currency, balance);

        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private String createCategory(String token, String name, String type, String color, String icon)
            throws Exception {
        String body = """
                {"name":"%s","type":"%s","color":"%s","icon":"%s"}
                """.formatted(name, type, color, icon);

        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    private void createTransaction(String token, String accountId, String categoryId, String type, String amount,
                                   String date, String description) throws Exception {
        String body = """
                {
                  "accountId":"%s",
                  "categoryId":"%s",
                  "type":"%s",
                  "amount":%s,
                  "currency":"USD",
                  "description":"%s",
                  "transactionDate":"%s"
                }
                """.formatted(accountId, categoryId, type, amount, description, date);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private String extractId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("id").asText();
    }
}
