package com.saveapenny.goal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.exception.GoalSimulationValidationException;
import com.saveapenny.goal.simulation.dto.ParsedGoalDraft;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class GoalPromptParser {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?i)(?:\\$\\s*([0-9][0-9,]*(?:\\.[0-9]+)?))|(?:([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(USD|EUR|TRY))");
    private static final Pattern YEARS_PATTERN = Pattern.compile("(?i)in\\s+(\\d+)\\s+years?");
    private static final Pattern MONTHS_PATTERN = Pattern.compile("(?i)in\\s+(\\d+)\\s+months?");

    private final ObjectMapper objectMapper;
    private final Clock assistantClock;

    public GoalPromptParser(ObjectMapper objectMapper, Clock assistantClock) {
        this.objectMapper = objectMapper;
        this.assistantClock = assistantClock;
    }

    public ParsedGoalDraft parse(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new GoalSimulationValidationException("Goal prompt is required.");
        }

        String normalized = prompt.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (!lower.contains("save")) {
            throw new GoalSimulationValidationException("Phase 4 currently supports savings prompts only.");
        }

        BigDecimal targetAmount = extractAmount(normalized);
        String currency = extractCurrency(normalized);
        LocalDate targetDate = extractTargetDate(normalized);

        ObjectNode values = objectMapper.createObjectNode();
        values.put("targetAmount", targetAmount);
        values.put("currency", currency);
        values.put("targetDate", targetDate.toString());
        values.put("expectedAnnualReturn", BigDecimal.ZERO);
        values.put("startBalance", BigDecimal.ZERO);

        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("version", 1);
        envelope.put("type", GoalType.SAVINGS.name());
        envelope.set("values", values);

        return ParsedGoalDraft.builder()
                .type(GoalType.SAVINGS)
                .title("Savings Goal")
                .targetAmount(targetAmount)
                .currency(currency)
                .targetDate(targetDate)
                .inputs(envelope)
                .build();
    }

    private BigDecimal extractAmount(String prompt) {
        Matcher matcher = AMOUNT_PATTERN.matcher(prompt);
        if (!matcher.find()) {
            throw new GoalSimulationValidationException("Could not determine target amount from the prompt.");
        }
        String raw = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        BigDecimal amount = new BigDecimal(raw.replace(",", ""));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GoalSimulationValidationException("Target amount must be positive.");
        }
        if (amount.compareTo(new BigDecimal("999999999")) > 0) {
            throw new GoalSimulationValidationException("Target amount is unrealistically large.");
        }
        return amount;
    }

    private String extractCurrency(String prompt) {
        Matcher matcher = AMOUNT_PATTERN.matcher(prompt);
        if (!matcher.find()) {
            throw new GoalSimulationValidationException("Could not determine goal currency from the prompt.");
        }
        if (matcher.group(1) != null) {
            return "USD";
        }
        if (matcher.group(3) != null) {
            return matcher.group(3).toUpperCase(Locale.ROOT);
        }
        throw new GoalSimulationValidationException("Could not determine goal currency from the prompt.");
    }

    private LocalDate extractTargetDate(String prompt) {
        Matcher years = YEARS_PATTERN.matcher(prompt);
        if (years.find()) {
            return LocalDate.now(assistantClock).plusYears(Long.parseLong(years.group(1)));
        }
        Matcher months = MONTHS_PATTERN.matcher(prompt);
        if (months.find()) {
            return LocalDate.now(assistantClock).plusMonths(Long.parseLong(months.group(1)));
        }
        throw new GoalSimulationValidationException("Could not determine the target horizon from the prompt.");
    }
}
