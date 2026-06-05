package com.saveapenny.ocr.application.extractor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DateParser {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2}|\\d{2}[/-]\\d{2}[/-]\\d{4})\\b");

    public LocalDate parseFirst(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = DATE_PATTERN.matcher(text);
        while (matcher.find()) {
            LocalDate parsed = parseToken(matcher.group(1));
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    public List<LocalDate> parseAll(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Matcher matcher = DATE_PATTERN.matcher(text);
        LinkedHashSet<LocalDate> results = new LinkedHashSet<>();
        while (matcher.find()) {
            LocalDate parsed = parseToken(matcher.group(1));
            if (parsed != null) {
                results.add(parsed);
            }
        }
        return new ArrayList<>(results);
    }

    private LocalDate parseToken(String value) {
        if (value.contains("-") && value.indexOf('-') == 4) {
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }

        String normalized = value.replace('-', '/');
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
