package com.saveapenny.mcp.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saveapenny.goal.dto.GoalRunResponse;
import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalRunTrigger;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.mcp.error.ToolExecutionException;
import com.saveapenny.mcp.error.ToolValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GoalToolMappingSupportTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void requireGoalId_throws_whenNull() {
        assertThrows(ToolValidationException.class, () ->
                GoalToolMappingSupport.requireGoalId(null, "goalId"));
    }

    @Test
    void requireGoalId_passes_whenNonNull() {
        GoalToolMappingSupport.requireGoalId(UUID.randomUUID(), "goalId");
    }

    @Test
    void notFound_createsException() {
        ToolExecutionException ex = GoalToolMappingSupport.notFound("Goal not found");
        assertEquals("Goal not found", ex.getMessage());
        assertNotNull(ex.getErrors());
    }

    @Test
    void classifyProgress_achieved_whenStatusAchieved() {
        assertEquals(GoalToolModels.ProgressStatus.ACHIEVED,
                GoalToolMappingSupport.classifyProgress(GoalStatus.ACHIEVED, BigDecimal.ZERO, BigDecimal.TEN, null));
    }

    @Test
    void classifyProgress_achieved_whenCurrentExceedsTarget() {
        assertEquals(GoalToolModels.ProgressStatus.ACHIEVED,
                GoalToolMappingSupport.classifyProgress(GoalStatus.ACTIVE, new BigDecimal("100"), new BigDecimal("50"), null));
    }

    @Test
    void classifyProgress_noProjection_whenProjectedNull() {
        assertEquals(GoalToolModels.ProgressStatus.NO_PROJECTION,
                GoalToolMappingSupport.classifyProgress(GoalStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.TEN, null));
    }

    @Test
    void classifyProgress_onTrack_whenProjectedExceedsTarget() {
        assertEquals(GoalToolModels.ProgressStatus.ON_TRACK,
                GoalToolMappingSupport.classifyProgress(GoalStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.TEN, new BigDecimal("15")));
    }

    @Test
    void classifyProgress_onTrack_whenProjectedEqualsTarget() {
        assertEquals(GoalToolModels.ProgressStatus.ON_TRACK,
                GoalToolMappingSupport.classifyProgress(GoalStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN));
    }

    @Test
    void classifyProgress_offTrack_whenGapRatioAbove10Percent() {
        assertEquals(GoalToolModels.ProgressStatus.OFF_TRACK,
                GoalToolMappingSupport.classifyProgress(GoalStatus.ACTIVE, BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("80")));
    }

    @Test
    void classifyProgress_atRisk_whenGapRatioBetween5And10Percent() {
        assertEquals(GoalToolModels.ProgressStatus.AT_RISK,
                GoalToolMappingSupport.classifyProgress(GoalStatus.ACTIVE, BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("93")));
    }

    @Test
    void classifyProgress_onTrack_whenGapRatioBelow5Percent() {
        assertEquals(GoalToolModels.ProgressStatus.ON_TRACK,
                GoalToolMappingSupport.classifyProgress(GoalStatus.ACTIVE, BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("97")));
    }

    @Test
    void classifyProgress_handlesZeroTarget() {
        assertEquals(GoalToolModels.ProgressStatus.ACHIEVED,
                GoalToolMappingSupport.classifyProgress(GoalStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.ZERO, null));
    }

    @Test
    void extractCurrentAmount_returnsZero_whenNullInput() {
        assertEquals(BigDecimal.ZERO, GoalToolMappingSupport.extractCurrentAmount(null));
    }

    @Test
    void extractCurrentAmount_returnsZero_whenNoValues() {
        ObjectNode node = mapper.createObjectNode();
        assertEquals(BigDecimal.ZERO, GoalToolMappingSupport.extractCurrentAmount(node));
    }

    @Test
    void extractCurrentAmount_readsStartBalance() {
        ObjectNode node = mapper.createObjectNode();
        node.set("values", mapper.createObjectNode().put("startBalance", 5000));
        assertEquals(new BigDecimal("5000"), GoalToolMappingSupport.extractCurrentAmount(node));
    }

    @Test
    void extractCurrentAmount_readsCurrentDownPayment() {
        ObjectNode node = mapper.createObjectNode();
        node.set("values", mapper.createObjectNode().put("currentDownPayment", 30000));
        assertEquals(new BigDecimal("30000"), GoalToolMappingSupport.extractCurrentAmount(node));
    }

    @Test
    void extractCurrentAmount_readsCurrentRetirementSavings() {
        ObjectNode node = mapper.createObjectNode();
        node.set("values", mapper.createObjectNode().put("currentRetirementSavings", 100000));
        assertEquals(new BigDecimal("100000"), GoalToolMappingSupport.extractCurrentAmount(node));
    }

    @Test
    void extractCurrentAmount_prioritizesStartBalance() {
        ObjectNode node = mapper.createObjectNode();
        ObjectNode values = mapper.createObjectNode();
        values.put("startBalance", 1000);
        values.put("currentDownPayment", 50000);
        node.set("values", values);
        assertEquals(new BigDecimal("1000"), GoalToolMappingSupport.extractCurrentAmount(node));
    }

    @Test
    void extractProjectedAmountAtTarget_returnsNull_whenNullRun() {
        assertNull(GoalToolMappingSupport.extractProjectedAmountAtTarget(null));
    }

    @Test
    void extractProjectedAmountAtTarget_returnsNull_whenNoSummary() {
        GoalRunResponse run = GoalRunResponse.builder().id(UUID.randomUUID()).build();
        assertNull(GoalToolMappingSupport.extractProjectedAmountAtTarget(run));
    }

    @Test
    void extractProjectedAmountAtTarget_readsProjectedAmount() {
        ObjectNode summary = mapper.createObjectNode();
        summary.put("projectedAmount", 50000);
        GoalRunResponse run = GoalRunResponse.builder()
                .id(UUID.randomUUID())
                .outputSummary(summary)
                .build();
        assertEquals(new BigDecimal("50000"), GoalToolMappingSupport.extractProjectedAmountAtTarget(run));
    }

    @Test
    void extractProjectedAmountAtTarget_readsProjectedNestEgg() {
        ObjectNode summary = mapper.createObjectNode();
        summary.put("projectedNestEgg", 750000);
        GoalRunResponse run = GoalRunResponse.builder()
                .id(UUID.randomUUID())
                .outputSummary(summary)
                .build();
        assertEquals(new BigDecimal("750000"), GoalToolMappingSupport.extractProjectedAmountAtTarget(run));
    }

    @Test
    void extractProjectedAmountAtTarget_prioritizesProjectedAmount() {
        ObjectNode summary = mapper.createObjectNode();
        summary.put("projectedAmount", 10000);
        summary.put("projectedNestEgg", 999999);
        GoalRunResponse run = GoalRunResponse.builder()
                .id(UUID.randomUUID())
                .outputSummary(summary)
                .build();
        assertEquals(new BigDecimal("10000"), GoalToolMappingSupport.extractProjectedAmountAtTarget(run));
    }

    @Test
    void monthsRemaining_returnsZero_whenNull() {
        assertEquals(0, GoalToolMappingSupport.monthsRemaining(null));
    }

    @Test
    void monthsRemaining_returnsZero_whenPastDate() {
        assertEquals(0, GoalToolMappingSupport.monthsRemaining(LocalDate.of(2020, 1, 1)));
    }

    @Test
    void monthsRemaining_returnsPositiveForFutureDate() {
        int result = GoalToolMappingSupport.monthsRemaining(LocalDate.of(2027, 6, 1));
        assertTrue(result > 0);
    }

    @Test
    void toGoalItem_mapsFields() {
        var goal = new com.saveapenny.goal.dto.GoalResponse();
        goal.setId(UUID.randomUUID());
        goal.setType(com.saveapenny.goal.entity.GoalType.SAVINGS);
        goal.setTitle("Test Goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setTargetAmount(new BigDecimal("10000"));
        goal.setCurrency("USD");
        goal.setTargetDate(LocalDate.of(2027, 1, 1));

        GoalToolModels.GoalItem item = GoalToolMappingSupport.toGoalItem(goal);

        assertEquals(goal.getId(), item.goalId());
        assertEquals("Test Goal", item.title());
        assertEquals(GoalStatus.ACTIVE, item.status());
        assertEquals(new BigDecimal("10000"), item.targetAmount());
    }

    @Test
    void toScenarioItem_mapsFields() {
        var scenario = new com.saveapenny.goal.dto.ScenarioResponse();
        scenario.setId(UUID.randomUUID());
        scenario.setName("Baseline");
        scenario.setIsBaseline(true);
        scenario.setCreatedAt(OffsetDateTime.now());

        GoalToolModels.ScenarioItem item = GoalToolMappingSupport.toScenarioItem(scenario);

        assertEquals(scenario.getId(), item.scenarioId());
        assertTrue(item.isBaseline());
    }

    @Test
    void toGoalRunItem_mapsFields() {
        var run = new com.saveapenny.goal.dto.GoalRunResponse();
        run.setId(UUID.randomUUID());
        run.setScenarioId(UUID.randomUUID());
        run.setFeasibility(Feasibility.ON_TRACK);
        run.setTriggeredBy(GoalRunTrigger.USER);
        run.setCreatedAt(OffsetDateTime.now());

        GoalToolModels.GoalRunItem item = GoalToolMappingSupport.toGoalRunItem(run);

        assertEquals(run.getId(), item.runId());
        assertEquals(Feasibility.ON_TRACK, item.feasibility());
    }
}
