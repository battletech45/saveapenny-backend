package com.saveapenny.analytics.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnalyticsEventTest {

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsEvent(" ", Map.of()));
    }

    @Test
    void rejectsNameWithInvalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsEvent("goal-achieved", Map.of()));
    }

    @Test
    void rejectsNameLongerThan40Characters() {
        String name = "a".repeat(41);
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsEvent(name, Map.of()));
    }

    @Test
    void rejectsMoreThan25Params() {
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < 26; i++) {
            params.put("param_" + i, i);
        }
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsEvent("goal_achieved", params));
    }

    @Test
    void rejectsParamKeyLongerThan40Characters() {
        Map<String, Object> params = Map.of("a".repeat(41), "value");
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsEvent("goal_achieved", params));
    }

    @Test
    void rejectsParamValueLongerThan100Characters() {
        Map<String, Object> params = Map.of("goal_id", "a".repeat(101));
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsEvent("goal_achieved", params));
    }

    @Test
    void acceptsParamValueAtExactly100Characters() {
        Map<String, Object> params = Map.of("goal_id", "a".repeat(100));
        AnalyticsEvent event = new AnalyticsEvent("goal_achieved", params);
        assertEquals("a".repeat(100), event.params().get("goal_id"));
    }

    @Test
    void defaultsToEmptyParamsWhenNull() {
        AnalyticsEvent event = new AnalyticsEvent("goal_achieved", null);
        assertTrue(event.params().isEmpty());
    }

    @Test
    void copiesParamsDefensively() {
        Map<String, Object> params = new HashMap<>();
        params.put("goal_id", "abc");
        AnalyticsEvent event = new AnalyticsEvent("goal_achieved", params);

        params.put("goal_id", "mutated");

        assertEquals("abc", event.params().get("goal_id"));
    }

    @Test
    void singleArgConstructorHasEmptyParams() {
        AnalyticsEvent event = new AnalyticsEvent("goal_achieved");
        assertTrue(event.params().isEmpty());
    }
}
