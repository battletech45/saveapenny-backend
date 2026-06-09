package com.saveapenny.goal.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.exception.GoalSimulationValidationException;
import com.saveapenny.goal.simulation.dto.ParsedGoalDraft;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalPromptParserTest {

    private Clock assistantClock;
    private ObjectMapper objectMapper;
    private GoalPromptParser parser;

    @BeforeEach
    void setUp() {
        assistantClock = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneId.of("UTC"));
        objectMapper = new ObjectMapper().findAndRegisterModules();
        parser = new GoalPromptParser(objectMapper, assistantClock);
    }

    @Test
    void parse_validSavingsPromptWithYears_parsesCorrectly() {
        ParsedGoalDraft draft = parser.parse("I want to save $10,000 in 3 years");

        assertNotNull(draft);
        assertEquals(GoalType.SAVINGS, draft.getType());
        assertEquals("Savings Goal", draft.getTitle());
        assertEquals(new BigDecimal("10000"), draft.getTargetAmount());
        assertEquals("USD", draft.getCurrency());
        assertEquals(LocalDate.of(2029, 6, 15), draft.getTargetDate());
    }

    @Test
    void parse_validSavingsPromptWithMonths_parsesCorrectly() {
        ParsedGoalDraft draft = parser.parse("Save 5000 EUR in 12 months");

        assertNotNull(draft);
        assertEquals(new BigDecimal("5000"), draft.getTargetAmount());
        assertEquals("EUR", draft.getCurrency());
        assertEquals(LocalDate.of(2027, 6, 15), draft.getTargetDate());
    }

    @Test
    void parse_validSavingsPromptWithTry_parsesCorrectly() {
        ParsedGoalDraft draft = parser.parse("Save 100000 TRY in 5 years");

        assertEquals(new BigDecimal("100000"), draft.getTargetAmount());
        assertEquals("TRY", draft.getCurrency());
        assertEquals(LocalDate.of(2031, 6, 15), draft.getTargetDate());
    }

    @Test
    void parse_blankPrompt_throwsException() {
        assertThrows(GoalSimulationValidationException.class, () -> parser.parse(""));
        assertThrows(GoalSimulationValidationException.class, () -> parser.parse(null));
        assertThrows(GoalSimulationValidationException.class, () -> parser.parse("   "));
    }

    @Test
    void parse_nonSavePrompt_throwsException() {
        assertThrows(GoalSimulationValidationException.class,
                () -> parser.parse("I want to invest 10000 USD in 2 years"));
    }

    @Test
    void parse_promptWithoutAmount_throwsException() {
        assertThrows(GoalSimulationValidationException.class,
                () -> parser.parse("I want to save money in 2 years"));
    }

    @Test
    void parse_promptWithoutHorizon_throwsException() {
        assertThrows(GoalSimulationValidationException.class,
                () -> parser.parse("I want to save 10000 USD"));
    }

    @Test
    void parse_promptWithDollarSignNoSpace_parsesCorrectly() {
        ParsedGoalDraft draft = parser.parse("save $20000 in 1 year");

        assertEquals(new BigDecimal("20000"), draft.getTargetAmount());
        assertEquals("USD", draft.getCurrency());
        assertEquals(LocalDate.of(2027, 6, 15), draft.getTargetDate());
    }

    @Test
    void parse_inputsEnvelope_containsVersionTypeAndValues() {
        ParsedGoalDraft draft = parser.parse("save $5,000.50 in 2 years");

        assertNotNull(draft.getInputs());
        assertEquals(1, draft.getInputs().get("version").intValue());
        assertEquals("SAVINGS", draft.getInputs().get("type").textValue());
        assertNotNull(draft.getInputs().get("values"));
        assertEquals(0, new BigDecimal("5000.50").compareTo(
                draft.getInputs().get("values").get("targetAmount").decimalValue()));
    }
}
