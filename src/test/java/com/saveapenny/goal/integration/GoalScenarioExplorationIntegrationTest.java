package com.saveapenny.goal.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:goal-scenario-exploration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class GoalScenarioExplorationIntegrationTest {

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
    void compareAndWhatIf_workWithoutPersistenceSideEffects() throws Exception {
        String token = registerAndGetToken("goal.phase5@example.com", "Goal Phase Five");
        String goalId = createGoal(token);

        String baselineScenarioId = extractFirstScenarioId(mockMvc.perform(get("/api/v1/goals/{goalId}", goalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());

        String aggressiveScenarioId = extractId(mockMvc.perform(post("/api/v1/goals/{goalId}/scenarios", goalId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                      "startBalance": 1000.0
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn());

        mockMvc.perform(post("/api/v1/goals/{goalId}/scenarios/compare", goalId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenarioIds": ["%s", "%s"]
                                }
                                """.formatted(baselineScenarioId, aggressiveScenarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarios.length()").value(2))
                .andExpect(jsonPath("$.data.deltas.length()").value(1));

        mockMvc.perform(post("/api/v1/goals/{goalId}/what-if", goalId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "overrides": {
                                    "monthlyContribution": 550.00
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projection").value(true))
                .andExpect(jsonPath("$.data.deltaVsBaseline.projectedAmountDelta").exists());

        mockMvc.perform(get("/api/v1/goals/{goalId}/scenarios", goalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/goals/{goalId}/runs", goalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    private String registerAndGetToken(String email, String fullName) throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Strong@123",
                                  "fullName": "%s"
                                }
                                """.formatted(email, fullName)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        return registerJson.path("data").path("accessToken").asText();
    }

    private String createGoal(String token) throws Exception {
        return extractId(mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "SAVINGS",
                                  "title": "House Fund",
                                  "targetAmount": 20000.0000,
                                  "currency": "USD",
                                  "targetDate": "2030-06-01",
                                  "inputs": {
                                    "version": 1,
                                    "type": "SAVINGS",
                                    "values": {
                                      "targetAmount": 20000.0000,
                                      "currency": "USD",
                                      "targetDate": "2030-06-01",
                                      "monthlyContribution": 300.0000,
                                      "expectedAnnualReturn": 0.0,
                                      "startBalance": 1000.0
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String extractId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("id").asText();
    }

    private String extractFirstScenarioId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("scenarios").get(0).path("id").asText();
    }
}
