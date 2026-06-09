package com.saveapenny.goal.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        "spring.datasource.url=jdbc:h2:mem:goal-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class GoalFlowIntegrationTest {

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
    void goalCrudFlow_worksForAuthenticatedUser() throws Exception {
        String token = registerAndGetToken("goal.flow@example.com", "Goal Flow");
        String accountId = createAccount(token);

        String createBody = """
                {
                  "type": "SAVINGS",
                  "title": "House Fund",
                  "targetAmount": 20000.0000,
                  "currency": "USD",
                  "targetDate": "2030-06-01",
                  "linkedAccountId": "%s",
                  "inputs": {
                    "version": 1,
                    "type": "SAVINGS",
                    "values": {
                      "targetAmount": 20000.0000,
                      "currency": "USD",
                      "targetDate": "2030-06-01",
                      "monthlyContribution": 300.0000,
                      "expectedAnnualReturn": 0.0,
                      "startBalance": 0.0
                    }
                  }
                }
                """.formatted(accountId);

        String goalId = extractId(mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn());

        mockMvc.perform(get("/api/v1/goals")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(goalId));

        mockMvc.perform(get("/api/v1/goals/{goalId}", goalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarios[0].name").value("Baseline"))
                .andExpect(jsonPath("$.data.latestRun").doesNotExist());

        String scenarioBody = """
                {
                  "name": "Aggressive",
                  "isBaseline": false,
                  "inputs": {
                    "version": 1,
                    "type": "SAVINGS",
                    "values": {
                      "targetAmount": 20000.0000,
                      "currency": "USD",
                      "targetDate": "2030-06-01",
                      "monthlyContribution": 500.0000,
                      "expectedAnnualReturn": 0.0,
                      "startBalance": 0.0
                    }
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/goals/{goalId}/scenarios", goalId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scenarioBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Aggressive"));

        mockMvc.perform(get("/api/v1/goals/{goalId}/scenarios", goalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/goals/{goalId}/runs", goalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        String updateBody = """
                {
                  "title": "Emergency Fund",
                  "currency": "EUR"
                }
                """;

        mockMvc.perform(patch("/api/v1/goals/{goalId}", goalId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Emergency Fund"))
                .andExpect(jsonPath("$.data.currency").value("EUR"));

        String statusBody = """
                {
                  "status": "ABANDONED"
                }
                """;

        mockMvc.perform(patch("/api/v1/goals/{goalId}/status", goalId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ABANDONED"));

        mockMvc.perform(delete("/api/v1/goals/{goalId}", goalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/goals/{goalId}", goalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("GOAL_NOT_FOUND"));
    }

    @Test
    void goalEndpoints_rejectCrossUserAccess() throws Exception {
        String ownerToken = registerAndGetToken("goal.owner@example.com", "Goal Owner");
        String attackerToken = registerAndGetToken("goal.attacker@example.com", "Goal Attacker");
        String goalId = extractId(mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "SAVINGS",
                                  "title": "Owner Goal",
                                  "targetAmount": 5000.0000,
                                  "currency": "USD",
                                  "targetDate": "2030-01-01",
                                  "inputs": {
                                    "version": 1,
                                    "type": "SAVINGS",
                                    "values": {
                                      "targetAmount": 5000.0000,
                                      "currency": "USD",
                                      "targetDate": "2030-01-01"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn());

        mockMvc.perform(get("/api/v1/goals/{goalId}", goalId)
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("GOAL_NOT_FOUND"));
    }

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

    private String createAccount(String token) throws Exception {
        String accountBody = """
                {
                  "name": "Cash",
                  "type": "CASH",
                  "currency": "USD",
                  "initialBalance": 1000.0000
                }
                """;

        return extractId(mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountBody))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String extractId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("id").asText();
    }
}
