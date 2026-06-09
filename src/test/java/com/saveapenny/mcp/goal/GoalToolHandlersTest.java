package com.saveapenny.mcp.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.goal.dto.GoalDetailResponse;
import com.saveapenny.goal.dto.GoalResponse;
import com.saveapenny.goal.dto.GoalRunResponse;
import com.saveapenny.goal.dto.ScenarioResponse;
import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalRunTrigger;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.exception.GoalNotFoundException;
import com.saveapenny.goal.service.GoalProgressCalculator;
import com.saveapenny.goal.service.GoalProgressReport;
import com.saveapenny.goal.service.GoalService;
import com.saveapenny.mcp.error.ToolExecutionException;
import com.saveapenny.mcp.error.ToolValidationException;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class GoalToolHandlersTest {

    @Mock
    private GoalService goalService;

    @Mock
    private GoalProgressCalculator goalProgressCalculator;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-08T00:00:00Z"), java.time.ZoneOffset.UTC);

    @Test
    void listGoals_returnsStructuredItems() {
        UUID userId = UUID.randomUUID();
        when(goalService.getAll(userId, null, null, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(GoalResponse.builder()
                        .id(UUID.randomUUID())
                        .type(GoalType.SAVINGS)
                        .title("House Fund")
                        .status(GoalStatus.ACTIVE)
                        .targetAmount(new BigDecimal("20000.00"))
                        .currency("USD")
                        .targetDate(LocalDate.of(2030, 6, 1))
                        .build())));

        ListGoalsToolResult result = new ListGoalsToolHandler(goalService)
                .execute(new ToolExecutionContext(userId), new ListGoalsToolInput(null, null, 10))
                .data();

        assertEquals(1, result.goals().size());
        assertEquals("House Fund", result.goals().getFirst().title());
    }

    @Test
    void getGoal_returnsGoalDetail() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalService.getById(userId, goalId)).thenReturn(goalDetail(goalId, true));

        GetGoalToolResult result = new GetGoalToolHandler(goalService)
                .execute(new ToolExecutionContext(userId), new GetGoalToolInput(goalId))
                .data();

        assertEquals(goalId, result.goal().goalId());
        assertEquals(1, result.goal().scenarios().size());
        assertEquals(Feasibility.ON_TRACK, result.goal().latestRun().feasibility());
    }

    @Test
    void getGoalProgress_returnsNoProjectionWhenNoRunExists() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalProgressCalculator.calculate(userId, goalId, LocalDate.now(clock)))
                .thenReturn(new GoalProgressReport(
                        goalId,
                        UUID.randomUUID(),
                        null,
                        new BigDecimal("5000.00"),
                        null,
                        null,
                        24,
                        GoalProgressReport.ProgressStatus.NO_PROJECTION,
                        0,
                        List.of(new GoalProgressReport.Warning("NO_PROJECTION", "No simulation run exists for this goal yet."))));

        GetGoalProgressToolResult result = new GetGoalProgressToolHandler(goalProgressCalculator, clock)
                .execute(new ToolExecutionContext(userId), new GetGoalProgressToolInput(goalId))
                .data();

        assertEquals(GoalToolModels.ProgressStatus.NO_PROJECTION, result.status());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void listGoalScenarios_rejectsMissingGoalId() {
        ListGoalScenariosToolHandler handler = new ListGoalScenariosToolHandler(goalService);

        assertThrows(ToolValidationException.class,
                () -> handler.execute(new ToolExecutionContext(UUID.randomUUID()), new ListGoalScenariosToolInput(null)));
    }

    @Test
    void listGoalRuns_translatesNotFoundToToolError() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalService.listRuns(userId, goalId, PageRequest.of(0, 10))).thenThrow(new GoalNotFoundException(goalId));

        ListGoalRunsToolHandler handler = new ListGoalRunsToolHandler(goalService);

        ToolExecutionException exception = assertThrows(
                ToolExecutionException.class,
                () -> handler.execute(new ToolExecutionContext(userId), new ListGoalRunsToolInput(goalId, 10)));

        assertEquals("NOT_FOUND", exception.getCode().name());
    }

    private GoalDetailResponse goalDetail(UUID goalId, boolean withRun) throws Exception {
        GoalRunResponse latestRun = withRun
                ? GoalRunResponse.builder()
                        .id(UUID.randomUUID())
                        .scenarioId(UUID.randomUUID())
                        .feasibility(Feasibility.ON_TRACK)
                        .triggeredBy(GoalRunTrigger.USER)
                        .createdAt(OffsetDateTime.now())
                        .outputSummary(objectMapper.readTree("{\"projectedAmount\":19000.00}"))
                        .build()
                : null;

        return GoalDetailResponse.builder()
                .id(goalId)
                .type(GoalType.SAVINGS)
                .title("House Fund")
                .status(GoalStatus.ACTIVE)
                .targetAmount(new BigDecimal("20000.00"))
                .currency("USD")
                .targetDate(LocalDate.now().plusMonths(24))
                .inputs(objectMapper.readTree("{\"version\":1,\"type\":\"SAVINGS\",\"values\":{\"startBalance\":5000.00}}"))
                .scenarios(List.of(ScenarioResponse.builder()
                        .id(UUID.randomUUID())
                        .goalId(goalId)
                        .name("Baseline")
                        .isBaseline(true)
                        .createdAt(OffsetDateTime.now())
                        .build()))
                .latestRun(latestRun)
                .build();
    }
}
