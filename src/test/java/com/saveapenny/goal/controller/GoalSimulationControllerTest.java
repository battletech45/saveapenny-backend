package com.saveapenny.goal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.exception.GoalSimulationValidationException;
import com.saveapenny.goal.service.GoalSimulationService;
import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.goal.simulation.dto.GoalSimulationPromptRequest;
import com.saveapenny.goal.simulation.dto.GoalSimulationResponse;
import com.saveapenny.goal.simulation.dto.ParsedGoalDraft;
import jakarta.servlet.FilterChain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GoalSimulationController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class GoalSimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GoalSimulationService goalSimulationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUpRateLimitingFilter() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
    }

    @Test
    void simulatePrompt_returnsDraftResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("goal-sim-token")).thenReturn(true);
        when(jwtService.extractUserId("goal-sim-token")).thenReturn(userId);
        when(goalSimulationService.simulatePrompt(eq(userId), eq("I want to save $20,000 in 3 years")))
                .thenReturn(GoalSimulationResponse.builder()
                        .parsedGoal(ParsedGoalDraft.builder()
                                .type(GoalType.SAVINGS)
                                .title("Savings Goal")
                                .targetAmount(new BigDecimal("20000.00"))
                                .currency("USD")
                                .targetDate(LocalDate.of(2029, 6, 1))
                                .build())
                        .result(SimulationResult.builder()
                                .version(1)
                                .type(GoalType.SAVINGS)
                                .feasibility(Feasibility.TIGHT)
                                .asOf(OffsetDateTime.now())
                                .horizonMonths(36)
                                .currency("USD")
                                .build())
                        .narrative("Simulation result: TIGHT.")
                        .disclaimer("disclaimer")
                        .draft(true)
                        .build());

        mockMvc.perform(post("/api/v1/goals/simulate")
                        .header("Authorization", "Bearer goal-sim-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoalSimulationPromptRequest("I want to save $20,000 in 3 years"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.parsedGoal.type").value("SAVINGS"))
                .andExpect(jsonPath("$.data.result.feasibility").value("TIGHT"));
    }

    @Test
    void unauthenticatedRequest_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/goals/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void simulatePrompt_returnsBadRequest_whenValidationFails() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-err-1")).thenReturn(true);
        when(jwtService.extractUserId("token-err-1")).thenReturn(userId);
        when(goalSimulationService.simulatePrompt(eq(userId), any()))
                .thenThrow(new GoalSimulationValidationException("Prompt is too vague"));

        mockMvc.perform(post("/api/v1/goals/simulate")
                        .header("Authorization", "Bearer token-err-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoalSimulationPromptRequest("Save some money"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_GOAL_SIMULATION_REQUEST"));
    }

    @Test
    void simulateExistingGoal_returnsSimulationResult() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("goal-sim-token-2")).thenReturn(true);
        when(jwtService.extractUserId("goal-sim-token-2")).thenReturn(userId);
        when(goalSimulationService.simulateGoal(eq(userId), eq(goalId), eq(null)))
                .thenReturn(SimulationResult.builder()
                        .version(1)
                        .type(GoalType.SAVINGS)
                        .feasibility(Feasibility.ON_TRACK)
                        .asOf(OffsetDateTime.now())
                        .horizonMonths(24)
                        .currency("USD")
                        .build());

        mockMvc.perform(post("/api/v1/goals/{goalId}/simulate", goalId)
                        .header("Authorization", "Bearer goal-sim-token-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feasibility").value("ON_TRACK"));
    }
}
