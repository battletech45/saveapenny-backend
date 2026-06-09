package com.saveapenny.mcp.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.goal.exception.GoalNotFoundException;
import com.saveapenny.goal.service.GoalSimulationService;
import com.saveapenny.goal.simulation.dto.GoalScenarioComparisonResponse;
import com.saveapenny.mcp.error.ToolExecutionException;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Phase5GoalToolHandlersTest {

    @Mock
    private GoalSimulationService goalSimulationService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void compareScenarios_returnsComparison() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalSimulationService.compareScenarios(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.eq(goalId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(GoalScenarioComparisonResponse.builder()
                        .goalId(goalId)
                        .scenarios(List.of(GoalScenarioComparisonResponse.ScenarioComparisonItem.builder()
                                .scenarioId(UUID.randomUUID())
                                .scenarioName("Baseline")
                                .feasibility("ON_TRACK")
                                .projectedAmount(new BigDecimal("5000.00"))
                                .build()))
                        .build());

        GoalScenarioComparisonResponse result = new CompareScenariosToolHandler(goalSimulationService)
                .execute(new ToolExecutionContext(userId), new CompareScenariosToolInput(goalId, null))
                .data();

        assertEquals(goalId, result.getGoalId());
        assertEquals(1, result.getScenarios().size());
    }

    @Test
    void whatIf_requiresOverrideObject() {
        WhatIfToolHandler handler = new WhatIfToolHandler(goalSimulationService);

        assertThrows(
                com.saveapenny.mcp.error.ToolValidationException.class,
                () -> handler.execute(new ToolExecutionContext(UUID.randomUUID()), new WhatIfToolInput(UUID.randomUUID(), null)));
    }

    @Test
    void whatIf_translatesNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalSimulationService.whatIf(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.eq(goalId), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new GoalNotFoundException(goalId));

        ToolExecutionException exception = assertThrows(
                ToolExecutionException.class,
                () -> new WhatIfToolHandler(goalSimulationService)
                        .execute(new ToolExecutionContext(userId), new WhatIfToolInput(goalId, objectMapper.readTree("{\"monthlyContribution\":500}"))));

        assertEquals("NOT_FOUND", exception.getCode().name());
    }
}
