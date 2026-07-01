package com.saveapenny.goal.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.test.TestcontainersIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:goal-simulation-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "security.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123"
})
class GoalSimulationFlowIntegrationTest extends TestcontainersIntegrationTest {

    @Test
    void simulateDraftPrompt_returnsSavingsSimulation() throws Exception {
        String token = registerAndGetToken("goal.simulation@example.com", "Goal Simulation");

        mockMvc.perform(post("/api/v1/goals/simulate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prompt": "I want to save $20,000 in 3 years"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parsedGoal.type").value("SAVINGS"))
                .andExpect(jsonPath("$.data.result.horizonMonths").value(36))
                .andExpect(jsonPath("$.data.disclaimer").exists());
    }

    @Test
    void simulateExistingGoal_returnsLiveSimulation() throws Exception {
        String token = registerAndGetToken("goal.simulation2@example.com", "Goal Simulation Two");
        String goalId = extractId(mockMvc.perform(post("/api/v1/goals")
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

        mockMvc.perform(post("/api/v1/goals/{goalId}/simulate", goalId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("SAVINGS"))
                .andExpect(jsonPath("$.data.summary.requiredMonthlyContribution").exists())
                .andExpect(jsonPath("$.data.series.length()").value(47));
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
}
