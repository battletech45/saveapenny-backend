package com.saveapenny.mcp.adapter.springai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.assistant.tool.AssistantToolContextHolder;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.mcp.budget.MonthlyBudgetStatusToolInput;
import com.saveapenny.mcp.budget.MonthlyBudgetStatusToolResult;
import com.saveapenny.mcp.goal.CompareScenariosToolInput;
import com.saveapenny.mcp.execution.ToolExecutionContext;
import com.saveapenny.mcp.execution.ToolHandler;
import com.saveapenny.mcp.execution.ToolResult;
import com.saveapenny.mcp.goal.GetGoalProgressToolInput;
import com.saveapenny.mcp.goal.GetGoalProgressToolResult;
import com.saveapenny.mcp.goal.GetGoalToolInput;
import com.saveapenny.mcp.goal.GetGoalToolResult;
import com.saveapenny.mcp.goal.GoalToolModels;
import com.saveapenny.mcp.goal.ListGoalRunsToolInput;
import com.saveapenny.mcp.goal.ListGoalRunsToolResult;
import com.saveapenny.mcp.goal.ListGoalScenariosToolInput;
import com.saveapenny.mcp.goal.ListGoalScenariosToolResult;
import com.saveapenny.mcp.goal.ListGoalsToolInput;
import com.saveapenny.mcp.goal.ListGoalsToolResult;
import com.saveapenny.mcp.goal.WhatIfToolInput;
import com.saveapenny.mcp.registry.ToolRegistry;
import com.saveapenny.mcp.error.ToolValidationException;
import com.saveapenny.mcp.report.CurrentMonthSummaryToolInput;
import com.saveapenny.mcp.report.CurrentMonthSummaryToolResult;
import com.saveapenny.mcp.report.TopSpendingCategoriesToolInput;
import com.saveapenny.mcp.report.TopSpendingCategoriesToolResult;
import com.saveapenny.mcp.transaction.RecentTransactionsToolInput;
import com.saveapenny.mcp.transaction.RecentTransactionsToolResult;
import com.saveapenny.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpringAiMcpToolAdapterTest {

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private AssistantToolContextHolder assistantToolContextHolder;

    @Mock
    private ToolHandler<CurrentMonthSummaryToolInput, CurrentMonthSummaryToolResult> currentMonthSummaryHandler;

    @Mock
    private ToolHandler<TopSpendingCategoriesToolInput, TopSpendingCategoriesToolResult> topSpendingCategoriesHandler;

    @Mock
    private ToolHandler<MonthlyBudgetStatusToolInput, MonthlyBudgetStatusToolResult> monthlyBudgetStatusHandler;

    @Mock
    private ToolHandler<RecentTransactionsToolInput, RecentTransactionsToolResult> recentTransactionsHandler;

    @Mock
    private ToolHandler<ListGoalsToolInput, ListGoalsToolResult> listGoalsHandler;

    @Mock
    private ToolHandler<GetGoalToolInput, GetGoalToolResult> getGoalHandler;

    @Mock
    private ToolHandler<GetGoalProgressToolInput, GetGoalProgressToolResult> getGoalProgressHandler;

    @Mock
    private ToolHandler<ListGoalScenariosToolInput, ListGoalScenariosToolResult> listGoalScenariosHandler;

    @Mock
    private ToolHandler<ListGoalRunsToolInput, ListGoalRunsToolResult> listGoalRunsHandler;

    @Mock
    private ToolHandler<CompareScenariosToolInput, com.saveapenny.goal.simulation.dto.GoalScenarioComparisonResponse> compareScenariosHandler;

    @Mock
    private ToolHandler<WhatIfToolInput, com.saveapenny.goal.simulation.dto.GoalWhatIfResponse> whatIfHandler;

    private SpringAiMcpToolAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SpringAiMcpToolAdapter(toolRegistry, assistantToolContextHolder);
    }

    @Test
    void getCurrentMonthSummary_formatsHandlerResultAndPassesUserContext() {
        UUID userId = UUID.randomUUID();
        when(assistantToolContextHolder.requireCurrentUserId()).thenReturn(userId);
        when(toolRegistry.findByName("getCurrentMonthSummary")).thenReturn(Optional.of(currentMonthSummaryHandler));
        when(currentMonthSummaryHandler.execute(any(ToolExecutionContext.class), any(CurrentMonthSummaryToolInput.class)))
                .thenReturn(ToolResult.of(new CurrentMonthSummaryToolResult(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 18),
                        new BigDecimal("3000.00"),
                        new BigDecimal("1800.00"),
                        new BigDecimal("1200.00"))));

        String result = adapter.getCurrentMonthSummary();

        ArgumentCaptor<ToolExecutionContext> contextCaptor = ArgumentCaptor.forClass(ToolExecutionContext.class);
        verify(currentMonthSummaryHandler).execute(contextCaptor.capture(), any(CurrentMonthSummaryToolInput.class));
        assertEquals(userId, contextCaptor.getValue().userId());
        assertTrue(result.contains("income=3000.00"));
        assertTrue(result.contains("expense=1800.00"));
        assertTrue(result.contains("netSavings=1200.00"));
    }

    @Test
    void getTopSpendingCategories_returnsFormattedCategoryLines() {
        when(assistantToolContextHolder.requireCurrentUserId()).thenReturn(UUID.randomUUID());
        when(toolRegistry.findByName("getTopSpendingCategories")).thenReturn(Optional.of(topSpendingCategoriesHandler));
        when(topSpendingCategoriesHandler.execute(any(ToolExecutionContext.class), any(TopSpendingCategoriesToolInput.class)))
                .thenReturn(ToolResult.of(new TopSpendingCategoriesToolResult(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 18),
                        List.of(
                                new TopSpendingCategoriesToolResult.TopSpendingCategoryItem(
                                        UUID.randomUUID(),
                                        "Food",
                                        new BigDecimal("600.00"),
                                        new BigDecimal("33.33")),
                                new TopSpendingCategoriesToolResult.TopSpendingCategoryItem(
                                        UUID.randomUUID(),
                                        "Transport",
                                        new BigDecimal("200.00"),
                                        new BigDecimal("11.11"))))));

        String result = adapter.getTopSpendingCategories(2);

        assertTrue(result.contains("Food=600.00 (33.33%)"));
        assertTrue(result.contains("Transport=200.00 (11.11%)"));
    }

    @Test
    void getMonthlyBudgetStatus_returnsNoneWhenNoBudgets() {
        when(assistantToolContextHolder.requireCurrentUserId()).thenReturn(UUID.randomUUID());
        when(toolRegistry.findByName("getMonthlyBudgetStatus")).thenReturn(Optional.of(monthlyBudgetStatusHandler));
        when(monthlyBudgetStatusHandler.execute(any(ToolExecutionContext.class), any(MonthlyBudgetStatusToolInput.class)))
                .thenReturn(ToolResult.of(new MonthlyBudgetStatusToolResult(List.of())));

        String result = adapter.getMonthlyBudgetStatus(3);

        assertEquals("Monthly budget status: none.", result);
    }

    @Test
    void getRecentTransactions_returnsFormattedTransactions() {
        when(assistantToolContextHolder.requireCurrentUserId()).thenReturn(UUID.randomUUID());
        when(toolRegistry.findByName("getRecentTransactions")).thenReturn(Optional.of(recentTransactionsHandler));
        when(recentTransactionsHandler.execute(any(ToolExecutionContext.class), any(RecentTransactionsToolInput.class)))
                .thenReturn(ToolResult.of(new RecentTransactionsToolResult(
                        LocalDate.of(2026, 4, 18),
                        LocalDate.of(2026, 5, 18),
                        List.of(
                                new RecentTransactionsToolResult.RecentTransactionItem(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        TransactionType.EXPENSE,
                                        new BigDecimal("45.00"),
                                        "TRY",
                                        "Coffee",
                                        LocalDate.of(2026, 5, 17))))));

        String result = adapter.getRecentTransactions(5);

        assertTrue(result.contains("EXPENSE 45.00 (Coffee)"));
    }

    @Test
    void throwsWhenRegistryDoesNotContainRequestedTool() {
        when(toolRegistry.findByName("getCurrentMonthSummary")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, adapter::getCurrentMonthSummary);

        assertTrue(exception.getMessage().contains("could not resolve tool"));
    }

    @Test
    void listGoals_formatsGoalSummaries() {
        when(assistantToolContextHolder.requireCurrentUserId()).thenReturn(UUID.randomUUID());
        when(toolRegistry.findByName("list_goals")).thenReturn(Optional.of(listGoalsHandler));
        when(listGoalsHandler.execute(any(ToolExecutionContext.class), any(ListGoalsToolInput.class)))
                .thenReturn(ToolResult.of(new ListGoalsToolResult(List.of(
                        new GoalToolModels.GoalItem(
                                UUID.randomUUID(), GoalType.SAVINGS, "House Fund", GoalStatus.ACTIVE,
                                new BigDecimal("20000.00"), "USD", LocalDate.of(2030, 6, 1))))));

        String result = adapter.listGoals(null, null, 5);

        assertTrue(result.contains("House Fund"));
        assertTrue(result.contains("SAVINGS"));
    }

    @Test
    void getGoal_formatsGoalDetail() {
        UUID goalId = UUID.randomUUID();
        when(assistantToolContextHolder.requireCurrentUserId()).thenReturn(UUID.randomUUID());
        when(toolRegistry.findByName("get_goal")).thenReturn(Optional.of(getGoalHandler));
        when(getGoalHandler.execute(any(ToolExecutionContext.class), any(GetGoalToolInput.class)))
                .thenReturn(ToolResult.of(new GetGoalToolResult(
                        new GoalToolModels.GoalDetailItem(
                                goalId,
                                GoalType.SAVINGS,
                                "House Fund",
                                GoalStatus.ACTIVE,
                                new BigDecimal("20000.00"),
                                "USD",
                                LocalDate.of(2030, 6, 1),
                                List.of(new GoalToolModels.ScenarioItem(UUID.randomUUID(), "Baseline", true, null)),
                                null))));

        String result = adapter.getGoal(goalId.toString());

        assertTrue(result.contains("House Fund"));
        assertTrue(result.contains("scenarios=1"));
    }

    @Test
    void compareScenarios_formatsScenarioComparison() {
        when(assistantToolContextHolder.requireCurrentUserId()).thenReturn(UUID.randomUUID());
        when(toolRegistry.findByName("compare_scenarios")).thenReturn(Optional.of(compareScenariosHandler));
        when(compareScenariosHandler.execute(any(ToolExecutionContext.class), any(CompareScenariosToolInput.class)))
                .thenReturn(ToolResult.of(com.saveapenny.goal.simulation.dto.GoalScenarioComparisonResponse.builder()
                        .scenarios(List.of(com.saveapenny.goal.simulation.dto.GoalScenarioComparisonResponse.ScenarioComparisonItem.builder()
                                .scenarioId(UUID.randomUUID())
                                .scenarioName("Baseline")
                                .feasibility("ON_TRACK")
                                .projectedAmount(new BigDecimal("5000.00"))
                                .build()))
                        .build()));

        String result = adapter.compareScenarios(UUID.randomUUID().toString());

        assertTrue(result.contains("Scenario comparison"));
        assertTrue(result.contains("Baseline"));
    }

    @Test
    void whatIf_formatsProjectionResponse() {
        when(assistantToolContextHolder.requireCurrentUserId()).thenReturn(UUID.randomUUID());
        when(toolRegistry.findByName("what_if")).thenReturn(Optional.of(whatIfHandler));
        when(whatIfHandler.execute(any(ToolExecutionContext.class), any(WhatIfToolInput.class)))
                .thenReturn(ToolResult.of(com.saveapenny.goal.simulation.dto.GoalWhatIfResponse.builder()
                        .result(com.saveapenny.goal.simulation.SimulationResult.builder()
                                .feasibility(com.saveapenny.goal.entity.Feasibility.ON_TRACK)
                                .summary(java.util.Map.of("projectedAmount", new BigDecimal("5200.00")))
                                .build())
                        .deltaVsBaseline(com.saveapenny.goal.simulation.dto.GoalWhatIfResponse.DeltaVsBaseline.builder()
                                .projectedAmountDelta(new BigDecimal("200.00"))
                                .build())
                        .build()));

        String result = adapter.whatIf(UUID.randomUUID().toString(), 500d);

        assertTrue(result.contains("What-if result"));
        assertTrue(result.contains("200.00"));
    }

    @Test
    void getGoal_rejectsInvalidGoalIdAsValidationError() {
        ToolValidationException exception = assertThrows(ToolValidationException.class, () -> adapter.getGoal("not-a-uuid"));

        assertEquals("goalId", exception.getErrors().getFirst().field());
    }

    @Test
    void listGoals_rejectsInvalidStatusAsValidationError() {
        ToolValidationException exception = assertThrows(ToolValidationException.class, () -> adapter.listGoals("bad-status", null, 5));

        assertEquals("status", exception.getErrors().getFirst().field());
    }

    @Test
    void getTopSpendingCategories_preservesInvalidLimitForHandlerValidation() {
        when(assistantToolContextHolder.requireCurrentUserId()).thenReturn(UUID.randomUUID());
        when(toolRegistry.findByName("getTopSpendingCategories")).thenReturn(Optional.of(topSpendingCategoriesHandler));
        when(topSpendingCategoriesHandler.execute(any(ToolExecutionContext.class), any(TopSpendingCategoriesToolInput.class)))
                .thenThrow(new ToolValidationException("limit must be greater than 0.", List.of()));

        assertThrows(ToolValidationException.class, () -> adapter.getTopSpendingCategories(0));
    }

    @Test
    void whatIf_preservesZeroMonthlyContributionOverride() {
        when(assistantToolContextHolder.requireCurrentUserId()).thenReturn(UUID.randomUUID());
        when(toolRegistry.findByName("what_if")).thenReturn(Optional.of(whatIfHandler));
        when(whatIfHandler.execute(any(ToolExecutionContext.class), any(WhatIfToolInput.class)))
                .thenReturn(ToolResult.of(com.saveapenny.goal.simulation.dto.GoalWhatIfResponse.builder()
                        .result(com.saveapenny.goal.simulation.SimulationResult.builder()
                                .feasibility(com.saveapenny.goal.entity.Feasibility.ON_TRACK)
                                .summary(java.util.Map.of("projectedAmount", new BigDecimal("5000.00")))
                                .build())
                        .build()));

        adapter.whatIf(UUID.randomUUID().toString(), 0d);

        ArgumentCaptor<WhatIfToolInput> inputCaptor = ArgumentCaptor.forClass(WhatIfToolInput.class);
        verify(whatIfHandler).execute(any(ToolExecutionContext.class), inputCaptor.capture());
        assertEquals(0d, inputCaptor.getValue().overrides().get("monthlyContribution").doubleValue());
    }
}
