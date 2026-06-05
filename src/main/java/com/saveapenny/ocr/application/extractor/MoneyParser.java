package com.saveapenny.ocr.application.extractor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class MoneyParser {

    private static final Pattern MONEY_PATTERN = Pattern.compile(
            "(?<!\\d)(?:[-+]\\s*)?(?:\\d{1,3}(?:[.,\\s]\\d{3})+|\\d+)(?:[.,]\\d{2})-?(?!\\d)");

    public BigDecimal parseFirst(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = MONEY_PATTERN.matcher(text);
        BigDecimal lastParsed = null;
        while (matcher.find()) {
            BigDecimal parsed = parseToken(matcher.group());
            if (parsed != null) {
                lastParsed = parsed;
            }
        }
        return lastParsed;
    }

    public List<BigDecimal> parseAll(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Matcher matcher = MONEY_PATTERN.matcher(text);
        LinkedHashSet<BigDecimal> results = new LinkedHashSet<>();
        while (matcher.find()) {
            BigDecimal parsed = parseToken(matcher.group());
            if (parsed != null) {
                results.add(parsed);
            }
        }
        return new ArrayList<>(results);
    }

    private BigDecimal parseToken(String rawToken) {
        String token = rawToken.replace(" ", "").trim();
        boolean negative = token.startsWith("-") || token.endsWith("-");
        if (token.startsWith("+") || token.startsWith("-")) {
            token = token.substring(1);
        }
        if (token.endsWith("-")) {
            token = token.substring(0, token.length() - 1);
        }

        int decimalIndex = Math.max(token.lastIndexOf(','), token.lastIndexOf('.'));
        if (decimalIndex < 0 || token.length() - decimalIndex - 1 != 2) {
            return null;
        }

        String integerPart = token.substring(0, decimalIndex).replaceAll("[.,]", "");
        String fractionPart = token.substring(decimalIndex + 1);
        String normalized = integerPart + "." + fractionPart;
        if (negative) {
            normalized = "-" + normalized;
        }

        try {
            return new BigDecimal(normalized);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
