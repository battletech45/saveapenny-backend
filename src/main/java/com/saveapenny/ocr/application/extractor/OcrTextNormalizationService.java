package com.saveapenny.ocr.application.extractor;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class OcrTextNormalizationService {

    public List<String> splitIntoNormalizedLines(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        return rawText.lines()
                .map(this::normalizeWhitespace)
                .filter(line -> !line.isBlank())
                .toList();
    }

    public List<List<String>> splitIntoNormalizedBlocks(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        List<List<String>> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String rawLine : rawText.lines().toList()) {
            String line = normalizeWhitespace(rawLine);
            if (line.isBlank()) {
                if (!current.isEmpty()) {
                    blocks.add(List.copyOf(current));
                    current.clear();
                }
                continue;
            }
            current.add(line);
        }
        if (!current.isEmpty()) {
            blocks.add(List.copyOf(current));
        }
        return List.copyOf(blocks);
    }

    public String normalizeWhitespace(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String normalizeForMatching(String value) {
        String normalized = normalizeWhitespace(value).toLowerCase(Locale.ROOT);
        String ascii = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return ascii.replace('ı', 'i');
    }
}
