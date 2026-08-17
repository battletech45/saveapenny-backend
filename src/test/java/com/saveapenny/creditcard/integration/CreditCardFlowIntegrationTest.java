package com.saveapenny.creditcard.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.category.entity.Category;
import com.saveapenny.category.entity.CategoryType;
import com.saveapenny.category.repository.CategoryRepository;
import com.saveapenny.creditcard.scheduler.CreditCardStatementScheduler;
import com.saveapenny.creditcard.support.CreditCardCategories;
import com.saveapenny.test.IntegrationTestBase;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:credit-card-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class CreditCardFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CreditCardStatementScheduler creditCardStatementScheduler;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void creditCardFlow_spendLimitStatementAndPayment() throws Exception {
        String accessToken = register("credit.flow@example.com", "Credit Flow");
        seedSystemCategories();

        String bankId = createAccount(accessToken, """
                {"name": "Checking", "type": "BANK", "currency": "USD", "initialBalance": 1000.0000}
                """);

        int statementDay = Math.min(LocalDate.now().getDayOfMonth(), 28);
        String creditBody = """
                {"name": "Visa", "type": "CREDIT", "currency": "USD", "initialBalance": 0.0000,
                 "creditLimit": 500.0000, "apr": 24.00, "statementDay": %d}
                """.formatted(statementDay);
        String creditId = createAccount(accessToken, creditBody);

        String expenseCategoryId = createExpenseCategory(accessToken);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "%s", "categoryId": "%s", "type": "EXPENSE",
                                 "amount": 500.0000, "currency": "USD", "transactionDate": "%s"}
                                """.formatted(creditId, expenseCategoryId, LocalDate.now())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": "%s", "categoryId": "%s", "type": "EXPENSE",
                                 "amount": 1.0000, "currency": "USD", "transactionDate": "%s"}
                                """.formatted(creditId, expenseCategoryId, LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CREDIT_LIMIT_EXCEEDED"));

        mockMvc.perform(get("/api/v1/accounts/{id}", creditId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(500.0));

        creditCardStatementScheduler.run();

        mockMvc.perform(get("/api/v1/accounts/{id}", creditId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.creditCard.currentStatementBalance").value(500.0))
                .andExpect(jsonPath("$.data.creditCard.minimumPaymentDue").value(25.0));

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/accounts/{id}/credit/payments", creditId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId": "%s", "paymentType": "MINIMUM_DUE"}
                                """.formatted(bankId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amountPaid").value(25.0))
                .andReturn();

        JsonNode paymentJson = objectMapper.readTree(paymentResult.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(475.0,
                paymentJson.path("data").path("remainingBalance").asDouble(), 0.0001);

        mockMvc.perform(get("/api/v1/accounts/{id}", bankId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(975.0));
    }

    private void seedSystemCategories() {
        if (categoryRepository.findById(CreditCardCategories.CREDIT_CARD_PAYMENT).isEmpty()) {
            categoryRepository.save(Category.builder()
                    .id(CreditCardCategories.CREDIT_CARD_PAYMENT)
                    .userId(null)
                    .name("Credit Card Payment")
                    .type(CategoryType.EXPENSE)
                    .build());
        }
        if (categoryRepository.findById(CreditCardCategories.INTEREST_AND_FEES).isEmpty()) {
            categoryRepository.save(Category.builder()
                    .id(CreditCardCategories.INTEREST_AND_FEES)
                    .userId(null)
                    .name("Interest & Fees")
                    .type(CategoryType.EXPENSE)
                    .build());
        }
    }

    private String createAccount(String accessToken, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asText();
    }

    private String createExpenseCategory(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Shopping", "type": "EXPENSE"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asText();
    }
}
