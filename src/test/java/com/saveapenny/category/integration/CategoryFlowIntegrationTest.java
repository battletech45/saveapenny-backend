package com.saveapenny.category.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.category.entity.Category;
import com.saveapenny.category.entity.CategoryType;
import com.saveapenny.category.repository.CategoryRepository;
import com.saveapenny.user.entity.Role;
import com.saveapenny.user.repository.RoleRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        "spring.datasource.url=jdbc:h2:mem:category-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class CategoryFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUpRole() {
        roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
    }

    @Test
    void categoryCrudFlow_worksForAuthenticatedUser() throws Exception {
        String registerBody = """
                {
                  "email": "category.flow@example.com",
                  "password": "Strong@123",
                  "fullName": "Category Flow"
                }
                """;

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String accessToken = registerJson.path("data").path("accessToken").asText();

        String createBody = """
                {
                  "name": "Food",
                  "type": "EXPENSE",
                  "color": "#ff0000",
                  "icon": "utensils"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Food"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String categoryId = created.path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/categories")
                        .param("type", "EXPENSE")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Food"));

        String updateBody = """
                {
                  "name": "Groceries",
                  "type": "EXPENSE",
                  "color": "#00ff00",
                  "icon": "basket"
                }
                """;

        mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Groceries"));

        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void getCategories_filteredByType_doesNotLeakSystemCategoriesOfOtherType() throws Exception {
        String registerBody = """
                {
                  "email": "category.type.filter@example.com",
                  "password": "Strong@123",
                  "fullName": "Category Type Filter"
                }
                """;

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String accessToken = registerJson.path("data").path("accessToken").asText();

        Category systemIncome = categoryRepository.save(Category.builder()
                .id(UUID.randomUUID())
                .userId(null)
                .name("Salary")
                .type(CategoryType.INCOME)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());
        Category systemExpense = categoryRepository.save(Category.builder()
                .id(UUID.randomUUID())
                .userId(null)
                .name("Interest & Fees")
                .type(CategoryType.EXPENSE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        MvcResult result = mockMvc.perform(get("/api/v1/categories")
                        .param("type", "INCOME")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        boolean containsIncomeSystemCategory = false;
        for (JsonNode item : data) {
            String id = item.path("id").asText();
            assertEquals("INCOME", item.path("type").asText(), "Every returned category must be of the requested type");
            assertFalse(id.equals(systemExpense.getId().toString()),
                    "EXPENSE-type system category must not leak into an INCOME-filtered response");
            if (id.equals(systemIncome.getId().toString())) {
                containsIncomeSystemCategory = true;
            }
        }
        assertTrue(containsIncomeSystemCategory, "INCOME-type system category should be present");
    }
}
