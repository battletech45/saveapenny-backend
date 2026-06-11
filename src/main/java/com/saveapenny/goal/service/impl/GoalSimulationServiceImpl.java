package com.saveapenny.goal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saveapenny.goal.dto.GoalDetailResponse;
import com.saveapenny.goal.dto.GoalRunResponse;
import com.saveapenny.goal.dto.ScenarioResponse;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.exception.GoalSimulationValidationException;
import com.saveapenny.goal.exception.ScenarioNotFoundException;
import com.saveapenny.goal.service.GoalContextProvider;
import com.saveapenny.goal.service.GoalService;
import com.saveapenny.goal.service.GoalSimulationService;
import com.saveapenny.goal.simulation.GoalContextSnapshot;
import com.saveapenny.goal.simulation.IncomeStrategy;
import com.saveapenny.goal.simulation.SimulationEngine;
import com.saveapenny.goal.simulation.SimulationInput;
import com.saveapenny.goal.simulation.SimulationResult;
import com.saveapenny.goal.simulation.dto.CompareScenariosRequest;
import com.saveapenny.goal.simulation.dto.DraftGoalSimulationRequest;
import com.saveapenny.goal.simulation.dto.GoalScenarioComparisonResponse;
import com.saveapenny.goal.simulation.dto.GoalSimulationResponse;
import com.saveapenny.goal.simulation.dto.GoalWhatIfResponse;
import com.saveapenny.goal.simulation.dto.ParsedGoalDraft;
import com.saveapenny.goal.simulation.dto.WhatIfRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GoalSimulationServiceImpl implements GoalSimulationService {

    private static final String DISCLAIMER = "This simulation is informational only and not financial, tax, legal, or investment advice.";

    private final GoalService goalService;
    private final GoalContextProvider goalContextProvider;
    private final GoalPromptParser goalPromptParser;
    private final ObjectMapper objectMapper;
    private final Clock assistantClock;
    private final SimulationEngine simulationEngine;

    public GoalSimulationServiceImpl(
            GoalService goalService,
            GoalContextProvider goalContextProvider,
            GoalPromptParser goalPromptParser,
            ObjectMapper objectMapper,
            Clock assistantClock) {
        this.goalService = goalService;
        this.goalContextProvider = goalContextProvider;
        this.goalPromptParser = goalPromptParser;
        this.objectMapper = objectMapper;
        this.assistantClock = assistantClock;
        this.simulationEngine = SimulationEngine.defaultEngine();
    }

    @Override
    public SimulationResult simulateGoal(UUID currentUserId, UUID goalId, UUID scenarioId) {
        GoalDetailResponse goal = goalService.getById(currentUserId, goalId);
        JsonNode effectiveInputs = goal.getInputs();
        if (scenarioId != null) {
            ScenarioResponse scenario = goal.getScenarios().stream()
                    .filter(item -> item.getId().equals(scenarioId))
                    .findFirst()
                    .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));
            effectiveInputs = mergeInputs(goal.getInputs(), scenario.getInputs());
        }
        return simulationEngine.simulate(toSimulationInput(goal.getType(), goal.getCurrency(), goal.getTargetAmount(), goal.getTargetDate(), effectiveInputs,
                goalContextProvider.getContext(currentUserId), false));
    }

    @Override
    public GoalSimulationResponse simulateDraft(UUID currentUserId, DraftGoalSimulationRequest request) {
        GoalContextSnapshot context = goalContextProvider.getContext(currentUserId);
        SimulationResult result = simulationEngine.simulate(toSimulationInput(
                request.getType(),
                request.getCurrency(),
                request.getTargetAmount(),
                request.getTargetDate(),
                request.getInputs(),
                context,
                false));

        ParsedGoalDraft draft = ParsedGoalDraft.builder()
                .type(request.getType())
                .title(request.getTitle())
                .targetAmount(request.getTargetAmount())
                .currency(request.getCurrency())
                .targetDate(request.getTargetDate())
                .inputs(request.getInputs())
                .build();

        return GoalSimulationResponse.builder()
                .goalId(null)
                .parsedGoal(draft)
                .result(result)
                .narrative(buildNarrative(result))
                .disclaimer(DISCLAIMER)
                .draft(true)
                .build();
    }

    @Override
    public GoalSimulationResponse simulatePrompt(UUID currentUserId, String prompt) {
        ParsedGoalDraft draft = goalPromptParser.parse(prompt);
        GoalContextSnapshot context = goalContextProvider.getContext(currentUserId);
        SimulationResult result = simulateWithInputs(
                draft.getType(),
                draft.getCurrency(),
                draft.getTargetAmount(),
                draft.getTargetDate(),
                draft.getInputs(),
                context,
                false);

        return GoalSimulationResponse.builder()
                .goalId(null)
                .parsedGoal(draft)
                .result(result)
                .narrative(buildNarrative(result))
                .disclaimer(DISCLAIMER)
                .draft(true)
                .build();
    }

    @Override
    public GoalScenarioComparisonResponse compareScenarios(UUID currentUserId, UUID goalId, CompareScenariosRequest request) {
        GoalDetailResponse goal = goalService.getById(currentUserId, goalId);
        GoalContextSnapshot context = goalContextProvider.getContext(currentUserId);
        List<ScenarioResponse> selected = resolveComparisonScenarios(goal, request);
        if (selected.size() > 10) {
            throw new GoalSimulationValidationException("A maximum of 10 scenarios can be compared at once.");
        }

        List<GoalScenarioComparisonResponse.ScenarioComparisonItem> scenarioItems = new ArrayList<>();
        List<GoalScenarioComparisonResponse.ScenarioDeltaItem> deltas = new ArrayList<>();
        GoalScenarioComparisonResponse.ScenarioComparisonItem baselineItem = null;

        for (ScenarioResponse scenario : selected) {
            SimulationResult result = simulateWithInputs(
                    goal.getType(),
                    goal.getCurrency(),
                    goal.getTargetAmount(),
                    goal.getTargetDate(),
                    mergeInputs(goal.getInputs(), scenario.getInputs()),
                    context,
                    false);
            GoalScenarioComparisonResponse.ScenarioComparisonItem item = toScenarioComparisonItem(scenario, result);
            scenarioItems.add(item);
            if (scenario.getIsBaseline()) {
                baselineItem = item;
            }
        }

        if (baselineItem != null) {
            for (GoalScenarioComparisonResponse.ScenarioComparisonItem item : scenarioItems) {
                if (!item.getScenarioId().equals(baselineItem.getScenarioId())) {
                    deltas.add(toScenarioDelta(baselineItem, item));
                }
            }
        }

        return GoalScenarioComparisonResponse.builder()
                .goalId(goalId)
                .scenarios(scenarioItems)
                .deltas(deltas)
                .disclaimer(DISCLAIMER)
                .build();
    }

    @Override
    public GoalWhatIfResponse whatIf(UUID currentUserId, UUID goalId, WhatIfRequest request) {
        GoalDetailResponse goal = goalService.getById(currentUserId, goalId);
        GoalContextSnapshot context = goalContextProvider.getContext(currentUserId);
        JsonNode overrides = request.getOverrides();
        if (overrides == null || !overrides.isObject()) {
            throw new GoalSimulationValidationException("What-if overrides must be an object.");
        }

        JsonNode mergedInputs = mergeFlatOverrides(goal.getInputs(), overrides);
        SimulationResult baselineResult = simulateWithInputs(
                goal.getType(), goal.getCurrency(), goal.getTargetAmount(), goal.getTargetDate(), goal.getInputs(), context, false);
        SimulationResult result = simulateWithInputs(
                goal.getType(), goal.getCurrency(), goal.getTargetAmount(), goal.getTargetDate(), mergedInputs, context, false);

        return GoalWhatIfResponse.builder()
                .goalId(goalId)
                .result(result)
                .deltaVsBaseline(toWhatIfDelta(baselineResult, result))
                .projection(true)
                .disclaimer(DISCLAIMER)
                .build();
    }

    private SimulationResult simulateWithInputs(
            GoalType goalType,
            String currency,
            BigDecimal targetAmount,
            LocalDate targetDate,
            JsonNode inputs,
            GoalContextSnapshot context,
            boolean linkedAccountMissing) {
        return simulationEngine.simulate(toSimulationInput(goalType, currency, targetAmount, targetDate, inputs, context, linkedAccountMissing));
    }

    private SimulationInput toSimulationInput(
            GoalType goalType,
            String currency,
            BigDecimal targetAmount,
            LocalDate targetDate,
            JsonNode inputs,
            GoalContextSnapshot context,
            boolean linkedAccountMissing) {
        JsonNode values = requireValues(inputs);
        SimulationInput.SimulationInputBuilder builder = SimulationInput.builder()
                .type(goalType)
                .asOfDate(LocalDate.now(assistantClock))
                .currency(currency)
                .primaryAccountCurrency(context.getPrimaryAccountCurrency())
                .missingIncomeHistory(context.isMissingIncomeHistory())
                .linkedAccountMissing(linkedAccountMissing)
                .averageMonthlyNetIncome(context.getAverageMonthlyNetIncome())
                .averageMonthlyExpense(context.getAverageMonthlyExpense())
                .targetAmount(targetAmount)
                .targetDate(targetDate);

        switch (goalType) {
            case SAVINGS -> builder
                    .monthlyContribution(decimal(values, "monthlyContribution"))
                    .expectedAnnualReturn(decimal(values, "expectedAnnualReturn"))
                    .startBalance(decimal(values, "startBalance"));
            case DEBT_PAYOFF -> builder
                    .currentBalance(decimal(values, "currentBalance"))
                    .apr(decimal(values, "apr"))
                    .minimumPayment(decimal(values, "minimumPayment"))
                    .monthlyBudget(decimal(values, "monthlyBudget"))
                    .fixedPayment(decimal(values, "fixedPayment"))
                    .targetPayoffDate(date(values, "targetPayoffDate"));
            case PURCHASE -> builder
                    .targetPrice(decimal(values, "targetPrice"))
                    .downPaymentPercent(decimal(values, "downPaymentPercent"))
                    .currentDownPayment(decimal(values, "currentDownPayment"))
                    .monthlySaving(decimal(values, "monthlySaving"))
                    .expectedAnnualReturn(decimal(values, "expectedAnnualReturn"))
                    .expectedPriceInflation(decimal(values, "expectedPriceInflation"));
            case RETIREMENT -> builder
                    .currentAge(integer(values, "currentAge"))
                    .targetRetirementAge(integer(values, "targetRetirementAge"))
                    .currentRetirementSavings(decimal(values, "currentRetirementSavings"))
                    .monthlyContribution(decimal(values, "monthlyContribution"))
                    .expectedAnnualReturn(decimal(values, "expectedAnnualReturn"))
                    .expectedInflation(decimal(values, "expectedInflation"))
                    .desiredMonthlyIncomeInRetirement(decimal(values, "desiredMonthlyIncomeInRetirement"))
                    .lifeExpectancy(integer(values, "lifeExpectancy"))
                    .withdrawalRate(decimal(values, "withdrawalRate"));
            case INCOME_TARGET -> builder
                    .targetMonthlyNetIncome(decimal(values, "targetMonthlyNetIncome"))
                    .currentAverageMonthlyNetIncome(decimal(values, "currentAverageMonthlyNetIncome"))
                    .expectedIncomeGrowthRate(decimal(values, "expectedIncomeGrowthRate"))
                    .incomeStrategy(enumValue(values, "incomeStrategy", IncomeStrategy.class));
        }
        return builder.build();
    }

    private JsonNode requireValues(JsonNode input) {
        if (input == null || !input.isObject() || input.get("values") == null || !input.get("values").isObject()) {
            throw new GoalSimulationValidationException("Simulation input must contain a valid values object.");
        }
        return input.get("values");
    }

    private JsonNode mergeInputs(JsonNode goalInputs, JsonNode scenarioInputs) {
        ObjectNode merged = goalInputs.deepCopy();
        JsonNode goalValues = merged.withObject("values");
        JsonNode scenarioValues = requireValues(scenarioInputs);
        scenarioValues.fields().forEachRemaining(entry -> ((ObjectNode) goalValues).set(entry.getKey(), entry.getValue()));
        return merged;
    }

    private JsonNode mergeFlatOverrides(JsonNode goalInputs, JsonNode overrides) {
        ObjectNode merged = goalInputs.deepCopy();
        ObjectNode goalValues = (ObjectNode) merged.withObject("values");
        overrides.fields().forEachRemaining(entry -> goalValues.set(entry.getKey(), entry.getValue()));
        return merged;
    }

    private BigDecimal decimal(JsonNode values, String field) {
        JsonNode node = values.get(field);
        return node != null && node.isNumber() ? node.decimalValue() : null;
    }

    private Integer integer(JsonNode values, String field) {
        JsonNode node = values.get(field);
        return node != null && node.canConvertToInt() ? node.intValue() : null;
    }

    private LocalDate date(JsonNode values, String field) {
        JsonNode node = values.get(field);
        if (node == null || !node.isTextual()) {
            return null;
        }
        try {
            return LocalDate.parse(node.textValue());
        } catch (DateTimeParseException ex) {
            throw new GoalSimulationValidationException("Field '%s' must be a valid ISO date.".formatted(field));
        }
    }

    private <E extends Enum<E>> E enumValue(JsonNode values, String field, Class<E> enumType) {
        JsonNode node = values.get(field);
        return node != null && node.isTextual() ? Enum.valueOf(enumType, node.textValue()) : null;
    }

    private String buildNarrative(SimulationResult result) {
        Object required = result.getSummary().get("requiredMonthlyContribution");
        if (required == null) {
            required = result.getSummary().get("requiredMonthlyGrowthRate");
        }
        Object projected = result.getSummary().get("projectedAmount");
        if (projected == null) {
            projected = result.getSummary().get("projectedNestEgg");
        }
        if (projected == null) {
            projected = result.getSummary().get("projectedMonthlyNetIncome");
        }
        return "Simulation result: " + result.getFeasibility()
                + ". Required change=" + required
                + ", projected outcome=" + projected
                + ", horizonMonths=" + result.getHorizonMonths() + '.';
    }

    private List<ScenarioResponse> resolveComparisonScenarios(GoalDetailResponse goal, CompareScenariosRequest request) {
        List<ScenarioResponse> scenarios = goal.getScenarios() == null ? List.of() : goal.getScenarios();
        if (request == null || request.getScenarioIds() == null || request.getScenarioIds().isEmpty()) {
            return scenarios.stream()
                    .sorted(Comparator.comparing(ScenarioResponse::getIsBaseline).reversed().thenComparing(ScenarioResponse::getCreatedAt))
                    .limit(10)
                    .toList();
        }
        return request.getScenarioIds().stream()
                .map(id -> scenarios.stream().filter(item -> item.getId().equals(id)).findFirst().orElseThrow(() -> new ScenarioNotFoundException(id)))
                .sorted(Comparator.comparing(ScenarioResponse::getIsBaseline).reversed().thenComparing(ScenarioResponse::getCreatedAt))
                .toList();
    }

    private GoalScenarioComparisonResponse.ScenarioComparisonItem toScenarioComparisonItem(ScenarioResponse scenario, SimulationResult result) {
        return GoalScenarioComparisonResponse.ScenarioComparisonItem.builder()
                .scenarioId(scenario.getId())
                .scenarioName(scenario.getName())
                .isBaseline(Boolean.TRUE.equals(scenario.getIsBaseline()))
                .feasibility(result.getFeasibility().name())
                .horizonMonths(result.getHorizonMonths())
                .currency(result.getCurrency())
                .requiredMonthlyContribution(decimalFromSummary(result, "requiredMonthlyContribution", "requiredMonthlyGrowthRate"))
                .projectedAmount(decimalFromSummary(result, "projectedAmount", "projectedNestEgg", "projectedMonthlyNetIncome"))
                .shortfall(decimalFromSummary(result, "shortfall"))
                .warningsCount(result.getWarnings().size())
                .build();
    }

    private GoalScenarioComparisonResponse.ScenarioDeltaItem toScenarioDelta(
            GoalScenarioComparisonResponse.ScenarioComparisonItem baseline,
            GoalScenarioComparisonResponse.ScenarioComparisonItem item) {
        return GoalScenarioComparisonResponse.ScenarioDeltaItem.builder()
                .fromScenarioId(baseline.getScenarioId())
                .toScenarioId(item.getScenarioId())
                .feasibilityChanged(!baseline.getFeasibility().equals(item.getFeasibility()))
                .requiredMonthlyContributionDelta(delta(item.getRequiredMonthlyContribution(), baseline.getRequiredMonthlyContribution()))
                .projectedAmountDelta(delta(item.getProjectedAmount(), baseline.getProjectedAmount()))
                .shortfallDelta(delta(item.getShortfall(), baseline.getShortfall()))
                .build();
    }

    private GoalWhatIfResponse.DeltaVsBaseline toWhatIfDelta(SimulationResult baseline, SimulationResult result) {
        return GoalWhatIfResponse.DeltaVsBaseline.builder()
                .requiredMonthlyContributionDelta(delta(
                        decimalFromSummary(result, "requiredMonthlyContribution", "requiredMonthlyGrowthRate"),
                        decimalFromSummary(baseline, "requiredMonthlyContribution", "requiredMonthlyGrowthRate")))
                .projectedAmountDelta(delta(
                        decimalFromSummary(result, "projectedAmount", "projectedNestEgg", "projectedMonthlyNetIncome"),
                        decimalFromSummary(baseline, "projectedAmount", "projectedNestEgg", "projectedMonthlyNetIncome")))
                .shortfallDelta(delta(
                        decimalFromSummary(result, "shortfall"),
                        decimalFromSummary(baseline, "shortfall")))
                .build();
    }

    private BigDecimal decimalFromSummary(SimulationResult result, String... fields) {
        for (String field : fields) {
            Object value = result.getSummary().get(field);
            if (value instanceof BigDecimal bigDecimal) {
                return bigDecimal;
            }
            if (value instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
            }
        }
        return null;
    }

    private BigDecimal delta(BigDecimal current, BigDecimal baseline) {
        if (current == null || baseline == null) {
            return null;
        }
        return current.subtract(baseline);
    }
}
