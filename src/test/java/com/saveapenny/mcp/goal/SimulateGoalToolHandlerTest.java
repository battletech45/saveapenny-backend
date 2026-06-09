package com.saveapenny.mcp.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.saveapenny.goal.exception.GoalNotFoundException;
import com.saveapenny.goal.service.GoalSimulationService;
import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.mcp.error.ToolExecutionException;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimulateGoalToolHandlerTest {

    @Mock
    private GoalSimulationService goalSimulationService;

    @Test
    void simulateGoal_returnsSimulationResult() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        SimulationResult result = SimulationResult.builder()
                .version(1)
                .type(GoalType.SAVINGS)
                .feasibility(Feasibility.ON_TRACK)
                .asOf(OffsetDateTime.now())
                .horizonMonths(36)
                .currency("USD")
                .build();
        when(goalSimulationService.simulateGoal(userId, goalId, null)).thenReturn(result);

        SimulationResult actual = new SimulateGoalToolHandler(goalSimulationService)
                .execute(new ToolExecutionContext(userId), new SimulateGoalToolInput(goalId, null))
                .data();

        assertEquals(Feasibility.ON_TRACK, actual.getFeasibility());
    }

    @Test
    void simulateGoal_translatesNotFound() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(goalSimulationService.simulateGoal(userId, goalId, null)).thenThrow(new GoalNotFoundException(goalId));

        ToolExecutionException exception = assertThrows(
                ToolExecutionException.class,
                () -> new SimulateGoalToolHandler(goalSimulationService)
                        .execute(new ToolExecutionContext(userId), new SimulateGoalToolInput(goalId, null)));

        assertEquals("NOT_FOUND", exception.getCode().name());
    }
}
