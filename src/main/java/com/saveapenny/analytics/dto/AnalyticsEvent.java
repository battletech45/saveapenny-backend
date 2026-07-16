package com.saveapenny.analytics.dto;

import java.util.Map;
import java.util.regex.Pattern;

public record AnalyticsEvent(String name, Map<String, Object> params) {

    private static final int MAX_NAME_LENGTH = 40;
    private static final int MAX_PARAM_COUNT = 25;
    private static final int MAX_PARAM_KEY_LENGTH = 40;
    private static final int MAX_PARAM_VALUE_LENGTH = 100;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    public AnalyticsEvent {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Analytics event name must not be blank");
        }
        if (name.length() > MAX_NAME_LENGTH || !NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Analytics event name is not a valid GA4 event name: " + name);
        }
        params = params == null ? Map.of() : Map.copyOf(params);
        if (params.size() > MAX_PARAM_COUNT) {
            throw new IllegalArgumentException("Analytics event '" + name + "' exceeds the maximum of " + MAX_PARAM_COUNT + " params");
        }
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getKey() == null || entry.getKey().length() > MAX_PARAM_KEY_LENGTH) {
                throw new IllegalArgumentException("Analytics event '" + name + "' has an invalid param key: " + entry.getKey());
            }
            String value = String.valueOf(entry.getValue());
            if (value.length() > MAX_PARAM_VALUE_LENGTH) {
                throw new IllegalArgumentException("Analytics event '" + name + "' param '" + entry.getKey()
                        + "' exceeds the maximum value length of " + MAX_PARAM_VALUE_LENGTH);
            }
        }
    }

    public AnalyticsEvent(String name) {
        this(name, Map.of());
    }
}
