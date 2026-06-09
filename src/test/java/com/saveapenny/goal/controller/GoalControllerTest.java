package com.saveapenny.goal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.auth.service.JwtService;
import com.saveapenny.config.security.HeaderUserAuthenticationFilter;
import com.saveapenny.config.security.RateLimitingFilter;
import com.saveapenny.config.security.SecurityConfig;
import com.saveapenny.goal.dto.CreateGoalRequest;
import com.saveapenny.goal.dto.CreateScenarioRequest;
import com.saveapenny.goal.dto.GoalDetailResponse;
import com.saveapenny.goal.dto.GoalResponse;
import com.saveapenny.goal.dto.GoalRunResponse;
import com.saveapenny.goal.dto.ScenarioResponse;
import com.saveapenny.goal.dto.UpdateGoalRequest;
import com.saveapenny.goal.dto.UpdateGoalStatusRequest;
import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalRunTrigger;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.service.GoalService;
import jakarta.servlet.FilterChain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GoalController.class)
@Import({SecurityConfig.class, HeaderUserAuthenticationFilter.class})
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GoalService goalService;

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
    void create_returnsCreatedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        GoalResponse response = sampleResponse();
        when(jwtService.isAccessTokenValid("token-1")).thenReturn(true);
        when(jwtService.extractUserId("token-1")).thenReturn(userId);
        when(goalService.create(eq(userId), any(CreateGoalRequest.class))).thenReturn(response);

        CreateGoalRequest request = CreateGoalRequest.builder()
                .type(GoalType.SAVINGS)
                .title("Emergency Fund")
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .build();

        mockMvc.perform(post("/api/v1/goals")
                        .header("Authorization", "Bearer token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Emergency Fund"));
    }

    @Test
    void getAll_returnsPagedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-2")).thenReturn(true);
        when(jwtService.extractUserId("token-2")).thenReturn(userId);
        when(goalService.getAll(eq(userId), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/goals")
                        .header("Authorization", "Bearer token-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Emergency Fund"));
    }

    @Test
    void getAll_withStatusFilter_passesFilter() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-2b")).thenReturn(true);
        when(jwtService.extractUserId("token-2b")).thenReturn(userId);
        when(goalService.getAll(eq(userId), eq(GoalStatus.ACTIVE), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/goals")
                        .header("Authorization", "Bearer token-2b")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getById_returnsDetailEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        GoalDetailResponse detail = GoalDetailResponse.builder()
                .id(goalId)
                .type(GoalType.SAVINGS)
                .title("Emergency Fund")
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .status(GoalStatus.ACTIVE)
                .inputs(objectMapper.readTree("{}"))
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .scenarios(List.of())
                .build();

        when(jwtService.isAccessTokenValid("token-3")).thenReturn(true);
        when(jwtService.extractUserId("token-3")).thenReturn(userId);
        when(goalService.getById(eq(userId), eq(goalId))).thenReturn(detail);

        mockMvc.perform(get("/api/v1/goals/{goalId}", goalId)
                        .header("Authorization", "Bearer token-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Emergency Fund"));
    }

    @Test
    void update_returnsUpdatedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        GoalResponse response = sampleResponse();
        response.setTitle("Updated Goal");

        when(jwtService.isAccessTokenValid("token-4")).thenReturn(true);
        when(jwtService.extractUserId("token-4")).thenReturn(userId);
        when(goalService.update(eq(userId), eq(goalId), any(UpdateGoalRequest.class))).thenReturn(response);

        UpdateGoalRequest request = UpdateGoalRequest.builder()
                .title("Updated Goal")
                .build();

        mockMvc.perform(patch("/api/v1/goals/{goalId}", goalId)
                        .header("Authorization", "Bearer token-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Goal"));
    }

    @Test
    void delete_returnsSuccessEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(jwtService.isAccessTokenValid("token-5")).thenReturn(true);
        when(jwtService.extractUserId("token-5")).thenReturn(userId);
        doNothing().when(goalService).delete(userId, goalId);

        mockMvc.perform(delete("/api/v1/goals/{goalId}", goalId)
                        .header("Authorization", "Bearer token-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void updateStatus_returnsUpdatedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        GoalResponse response = sampleResponse();
        response.setStatus(GoalStatus.ACHIEVED);

        when(jwtService.isAccessTokenValid("token-6")).thenReturn(true);
        when(jwtService.extractUserId("token-6")).thenReturn(userId);
        when(goalService.updateStatus(eq(userId), eq(goalId), eq(GoalStatus.ACHIEVED))).thenReturn(response);

        UpdateGoalStatusRequest request = new UpdateGoalStatusRequest();
        request.setStatus(GoalStatus.ACHIEVED);

        mockMvc.perform(patch("/api/v1/goals/{goalId}/status", goalId)
                        .header("Authorization", "Bearer token-6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACHIEVED"));
    }

    @Test
    void createScenario_returnsCreatedEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        ScenarioResponse scenario = ScenarioResponse.builder()
                .id(UUID.randomUUID())
                .goalId(goalId)
                .name("Aggressive")
                .inputs(objectMapper.readTree("{\"monthlyContribution\": 500}"))
                .isBaseline(false)
                .createdAt(OffsetDateTime.now())
                .build();

        when(jwtService.isAccessTokenValid("token-7")).thenReturn(true);
        when(jwtService.extractUserId("token-7")).thenReturn(userId);
        when(goalService.createScenario(eq(userId), eq(goalId), any(CreateScenarioRequest.class)))
                .thenReturn(scenario);

        CreateScenarioRequest request = new CreateScenarioRequest();
        request.setName("Aggressive");
        request.setInputs(objectMapper.readTree("{\"monthlyContribution\": 500}"));
        request.setIsBaseline(false);

        mockMvc.perform(post("/api/v1/goals/{goalId}/scenarios", goalId)
                        .header("Authorization", "Bearer token-7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Aggressive"));
    }

    @Test
    void listScenarios_returnsScenarios() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        ScenarioResponse scenario = ScenarioResponse.builder()
                .id(UUID.randomUUID())
                .goalId(goalId)
                .name("Baseline")
                .inputs(objectMapper.readTree("{}"))
                .isBaseline(true)
                .createdAt(OffsetDateTime.now())
                .build();

        when(jwtService.isAccessTokenValid("token-8")).thenReturn(true);
        when(jwtService.extractUserId("token-8")).thenReturn(userId);
        when(goalService.listScenarios(eq(userId), eq(goalId))).thenReturn(List.of(scenario));

        mockMvc.perform(get("/api/v1/goals/{goalId}/scenarios", goalId)
                        .header("Authorization", "Bearer token-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Baseline"));
    }

    @Test
    void listRuns_returnsPagedRuns() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        GoalRunResponse run = GoalRunResponse.builder()
                .id(UUID.randomUUID())
                .goalId(goalId)
                .feasibility(Feasibility.ON_TRACK)
                .triggeredBy(GoalRunTrigger.USER)
                .createdAt(OffsetDateTime.now())
                .build();

        when(jwtService.isAccessTokenValid("token-9")).thenReturn(true);
        when(jwtService.extractUserId("token-9")).thenReturn(userId);
        when(goalService.listRuns(eq(userId), eq(goalId), any()))
                .thenReturn(new PageImpl<>(List.of(run)));

        mockMvc.perform(get("/api/v1/goals/{goalId}/runs", goalId)
                        .header("Authorization", "Bearer token-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].feasibility").value("ON_TRACK"));
    }

    @Test
    void unauthenticatedRequest_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/goals"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    private GoalResponse sampleResponse() {
        return GoalResponse.builder()
                .id(UUID.randomUUID())
                .type(GoalType.SAVINGS)
                .title("Emergency Fund")
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .status(GoalStatus.ACTIVE)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
