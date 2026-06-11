package com.saveapenny.goal.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.goal.dto.GoalDetailResponse;
import com.saveapenny.goal.dto.ScenarioResponse;
import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.exception.GoalSimulationValidationException;
import com.saveapenny.goal.exception.ScenarioNotFoundException;
import com.saveapenny.goal.service.GoalContextProvider;
import com.saveapenny.goal.service.GoalService;
import com.saveapenny.goal.simulation.GoalContextSnapshot;
import com.saveapenny.goal.simulation.SimulationEngine;
import com.saveapenny.goal.simulation.SimulationInput;
import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.goal.simulation.dto.CompareScenariosRequest;
import com.saveapenny.goal.simulation.dto.DraftGoalSimulationRequest;
import com.saveapenny.goal.simulation.dto.GoalScenarioComparisonResponse;
import com.saveapenny.goal.simulation.dto.GoalSimulationResponse;
import com.saveapenny.goal.simulation.dto.ParsedGoalDraft;
import com.saveapenny.goal.simulation.dto.WhatIfRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GoalSimulationServiceImplTest {

    @Mock
    private GoalService goalService;

    @Mock
    private GoalContextProvider goalContextProvider;

    @Mock
    private GoalPromptParser goalPromptParser;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Clock assistantClock;

    @InjectMocks
    private GoalSimulationServiceImpl goalSimulationService;

    private final ObjectMapper realObjectMapper = new ObjectMapper().findAndRegisterModules();
    private SimulationEngine mockEngine;
    private UUID userId;
    private UUID goalId;
    private UUID scenarioId;
    private GoalContextSnapshot context;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        goalId = UUID.randomUUID();
        scenarioId = UUID.randomUUID();

        context = GoalContextSnapshot.builder()
                .primaryAccountCurrency("USD")
                .averageMonthlyNetIncome(new BigDecimal("5000"))
                .averageMonthlyExpense(new BigDecimal("2500"))
                .missingIncomeHistory(false)
                .build();

        mockEngine = org.mockito.Mockito.mock(SimulationEngine.class);
        ReflectionTestUtils.setField(goalSimulationService, "simulationEngine", mockEngine);

        lenient().when(assistantClock.instant()).thenReturn(Instant.parse("2026-06-15T10:00:00Z"));
        lenient().when(assistantClock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void simulatePrompt_parsesAndSimulates() {
        String prompt = "I want to save $10,000 in 2 years";
        ParsedGoalDraft draft = ParsedGoalDraft.builder()
                .type(GoalType.SAVINGS)
                .title("Save $10k")
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2028, 6, 1))
                .inputs(realObjectMapper.createObjectNode()
                        .set("values", realObjectMapper.createObjectNode()
                                .put("monthlyContribution", 400)))
                .build();

        SimulationResult expectedResult = SimulationResult.builder()
                .feasibility(Feasibility.ON_TRACK)
                .summary(java.util.Map.of("requiredMonthlyContribution", new BigDecimal("400"),
                        "projectedAmount", new BigDecimal("10500")))
                .build();

        when(goalPromptParser.parse(prompt)).thenReturn(draft);
        when(goalContextProvider.getContext(userId)).thenReturn(context);
        when(mockEngine.simulate(any(com.saveapenny.goal.simulation.SimulationInput.class))).thenReturn(expectedResult);

        GoalSimulationResponse response = goalSimulationService.simulatePrompt(userId, prompt);

        assertNotNull(response);
        assertEquals(Feasibility.ON_TRACK, response.getResult().getFeasibility());
        assertEquals("Save $10k", response.getParsedGoal().getTitle());
    }

    @Test
    void simulateGoal_returnsResultForBaseline() {
        GoalDetailResponse goal = GoalDetailResponse.builder()
                .id(goalId)
                .type(GoalType.SAVINGS)
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .inputs(realObjectMapper.createObjectNode()
                        .set("values", realObjectMapper.createObjectNode()
                                .put("monthlyContribution", 300)
                                .put("expectedAnnualReturn", 5)))
                .scenarios(List.of())
                .build();

        SimulationResult expectedResult = SimulationResult.builder()
                .feasibility(Feasibility.ON_TRACK)
                .build();

        when(goalService.getById(userId, goalId)).thenReturn(goal);
        when(goalContextProvider.getContext(userId)).thenReturn(context);
        when(mockEngine.simulate(any(SimulationInput.class))).thenReturn(expectedResult);

        SimulationResult result = goalSimulationService.simulateGoal(userId, goalId, null);

        assertNotNull(result);
        assertEquals(Feasibility.ON_TRACK, result.getFeasibility());
    }

    @Test
    void simulateGoal_withScenario_appliesScenarioOverrides() {
        JsonNode scenarioInputs = realObjectMapper.createObjectNode()
                .set("values", realObjectMapper.createObjectNode()
                        .put("monthlyContribution", 500));

        ScenarioResponse scenario = ScenarioResponse.builder()
                .id(scenarioId)
                .name("Aggressive")
                .isBaseline(false)
                .inputs(scenarioInputs)
                .createdAt(OffsetDateTime.now())
                .build();

        GoalDetailResponse goal = GoalDetailResponse.builder()
                .id(goalId)
                .type(GoalType.SAVINGS)
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .inputs(realObjectMapper.createObjectNode()
                        .set("values", realObjectMapper.createObjectNode()
                                .put("monthlyContribution", 300)
                                .put("expectedAnnualReturn", 5)))
                .scenarios(List.of(scenario))
                .build();

        SimulationResult expectedResult = SimulationResult.builder()
                .feasibility(Feasibility.ON_TRACK)
                .build();

        when(goalService.getById(userId, goalId)).thenReturn(goal);
        when(goalContextProvider.getContext(userId)).thenReturn(context);
        when(mockEngine.simulate(any(SimulationInput.class))).thenReturn(expectedResult);

        SimulationResult result = goalSimulationService.simulateGoal(userId, goalId, scenarioId);

        assertNotNull(result);
        assertEquals(Feasibility.ON_TRACK, result.getFeasibility());
    }

    @Test
    void simulateGoal_withUnknownScenario_throwsException() {
        GoalDetailResponse goal = GoalDetailResponse.builder()
                .id(goalId)
                .type(GoalType.SAVINGS)
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .inputs(realObjectMapper.createObjectNode()
                        .set("values", realObjectMapper.createObjectNode()
                                .put("monthlyContribution", 300)))
                .scenarios(List.of())
                .build();

        when(goalService.getById(userId, goalId)).thenReturn(goal);
        // no stubbings for goalContextProvider or mockEngine — method throws before reaching them

        assertThrows(ScenarioNotFoundException.class,
                () -> goalSimulationService.simulateGoal(userId, goalId, scenarioId));
    }

    @Test
    void simulateDraft_returnsSimulationResponse() {
        DraftGoalSimulationRequest request = DraftGoalSimulationRequest.builder()
                .type(GoalType.SAVINGS)
                .title("Test Goal")
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .inputs(realObjectMapper.createObjectNode()
                        .set("values", realObjectMapper.createObjectNode()
                                .put("monthlyContribution", 300)
                                .put("expectedAnnualReturn", 5)
                                .put("startBalance", 0)))
                .build();

        SimulationResult expectedResult = SimulationResult.builder()
                .feasibility(Feasibility.TIGHT)
                .summary(java.util.Map.of("requiredMonthlyContribution", new BigDecimal("300"),
                        "projectedAmount", new BigDecimal("9500")))
                .build();

        when(goalContextProvider.getContext(userId)).thenReturn(context);
        when(mockEngine.simulate(any(SimulationInput.class))).thenReturn(expectedResult);

        GoalSimulationResponse response = goalSimulationService.simulateDraft(userId, request);

        assertNotNull(response);
        assertEquals(Feasibility.TIGHT.name(), response.getResult().getFeasibility().name());
    }

    @Test
    void compareScenarios_returnsComparisonWithDeltas() {
        ScenarioResponse baseline = ScenarioResponse.builder()
                .id(scenarioId)
                .name("Baseline")
                .isBaseline(true)
                .inputs(realObjectMapper.createObjectNode()
                        .set("values", realObjectMapper.createObjectNode()
                                .put("monthlyContribution", 300)))
                .createdAt(OffsetDateTime.now().minusDays(1))
                .build();

        UUID altId = UUID.randomUUID();
        ScenarioResponse alternative = ScenarioResponse.builder()
                .id(altId)
                .name("Aggressive")
                .isBaseline(false)
                .inputs(realObjectMapper.createObjectNode()
                        .set("values", realObjectMapper.createObjectNode()
                                .put("monthlyContribution", 500)))
                .createdAt(OffsetDateTime.now())
                .build();

        GoalDetailResponse goal = GoalDetailResponse.builder()
                .id(goalId)
                .type(GoalType.SAVINGS)
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .inputs(realObjectMapper.createObjectNode()
                        .set("values", realObjectMapper.createObjectNode()
                                .put("monthlyContribution", 300)
                                .put("expectedAnnualReturn", 5)))
                .scenarios(List.of(baseline, alternative))
                .build();

        SimulationResult baseResult = SimulationResult.builder()
                .feasibility(Feasibility.TIGHT)
                .summary(java.util.Map.of("requiredMonthlyContribution", new BigDecimal("300"),
                        "projectedAmount", new BigDecimal("9500")))
                .build();

        SimulationResult altResult = SimulationResult.builder()
                .feasibility(Feasibility.ON_TRACK)
                .summary(java.util.Map.of("requiredMonthlyContribution", new BigDecimal("250"),
                        "projectedAmount", new BigDecimal("10500")))
                .build();

        when(goalService.getById(userId, goalId)).thenReturn(goal);
        when(goalContextProvider.getContext(userId)).thenReturn(context);
        when(mockEngine.simulate(any(SimulationInput.class)))
                .thenReturn(baseResult)
                .thenReturn(altResult);

        GoalScenarioComparisonResponse response = goalSimulationService.compareScenarios(
                userId, goalId, CompareScenariosRequest.builder().build());

        assertNotNull(response);
        assertEquals(2, response.getScenarios().size());
        assertEquals(1, response.getDeltas().size());
    }

    @Test
    void compareScenarios_tooManyScenarios_throwsException() {
        List<UUID> manyIds = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> UUID.randomUUID())
                .toList();

        GoalDetailResponse goal = GoalDetailResponse.builder()
                .id(goalId)
                .type(GoalType.SAVINGS)
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .inputs(realObjectMapper.createObjectNode())
                .scenarios(manyIds.stream().map(id -> ScenarioResponse.builder()
                        .id(id)
                        .name("S" + id)
                        .isBaseline(false)
                        .inputs(realObjectMapper.createObjectNode()
                                .set("values", realObjectMapper.createObjectNode()
                                        .put("monthlyContribution", 100)))
                        .createdAt(OffsetDateTime.now())
                        .build()).toList())
                .build();

        when(goalService.getById(userId, goalId)).thenReturn(goal);

        CompareScenariosRequest request = CompareScenariosRequest.builder()
                .scenarioIds(manyIds)
                .build();

        assertThrows(GoalSimulationValidationException.class,
                () -> goalSimulationService.compareScenarios(userId, goalId, request));
    }

    @Test
    void whatIf_withNullOverrides_throwsException() {
        GoalDetailResponse goal = GoalDetailResponse.builder()
                .id(goalId)
                .type(GoalType.SAVINGS)
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .inputs(realObjectMapper.createObjectNode()
                        .set("values", realObjectMapper.createObjectNode()
                                .put("monthlyContribution", 300)))
                .scenarios(List.of())
                .build();

        when(goalService.getById(userId, goalId)).thenReturn(goal);
        // mockEngine is not needed — the method validates overrides before simulating

        WhatIfRequest request = WhatIfRequest.builder().overrides(null).build();

        assertThrows(GoalSimulationValidationException.class,
                () -> goalSimulationService.whatIf(userId, goalId, request));
    }

    @Test
    void whatIf_withValidOverrides_returnsResponse() {
        GoalDetailResponse goal = GoalDetailResponse.builder()
                .id(goalId)
                .type(GoalType.SAVINGS)
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .inputs(realObjectMapper.createObjectNode()
                        .set("values", realObjectMapper.createObjectNode()
                                .put("monthlyContribution", 300)
                                .put("expectedAnnualReturn", 5)))
                .scenarios(List.of())
                .build();

        SimulationResult baseResult = SimulationResult.builder()
                .feasibility(Feasibility.TIGHT)
                .summary(java.util.Map.of("requiredMonthlyContribution", new BigDecimal("300"),
                        "projectedAmount", new BigDecimal("9500")))
                .build();

        SimulationResult overrideResult = SimulationResult.builder()
                .feasibility(Feasibility.ON_TRACK)
                .summary(java.util.Map.of("requiredMonthlyContribution", new BigDecimal("200"),
                        "projectedAmount", new BigDecimal("11000")))
                .build();

        JsonNode overrides = realObjectMapper.createObjectNode()
                .put("monthlyContribution", 500);

        when(goalService.getById(userId, goalId)).thenReturn(goal);
        when(goalContextProvider.getContext(userId)).thenReturn(context);
        when(mockEngine.simulate(any(SimulationInput.class)))
                .thenReturn(baseResult)
                .thenReturn(overrideResult);

        var response = goalSimulationService.whatIf(userId, goalId,
                WhatIfRequest.builder().overrides(overrides).build());

        assertNotNull(response);
        assertNotNull(response.getDeltaVsBaseline());
    }

    @Test
    void simulateDraft_withInvalidDate_throwsValidationException() {
        DraftGoalSimulationRequest request = DraftGoalSimulationRequest.builder()
                .type(GoalType.DEBT_PAYOFF)
                .title("Bad Date")
                .targetAmount(new BigDecimal("10000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 12, 31))
                .inputs(realObjectMapper.createObjectNode()
                        .set("values", realObjectMapper.createObjectNode()
                                .put("targetPayoffDate", "2024-13-01")
                                .put("currentBalance", 1000)
                                .put("apr", 5)
                                .put("monthlyPayment", 100)))
                .build();

        when(goalContextProvider.getContext(userId)).thenReturn(context);

        assertThrows(GoalSimulationValidationException.class, () -> goalSimulationService.simulateDraft(userId, request));
    }
}
