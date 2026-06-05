package com.saveapenny.ocr.application.parser.strategy;

import com.saveapenny.ocr.application.analysis.OcrDocumentAnalysisSeed;
import com.saveapenny.ocr.application.analysis.OcrDocumentBlock;
import com.saveapenny.ocr.application.analysis.OcrScoredCandidate;
import com.saveapenny.ocr.application.extractor.MoneyParser;
import com.saveapenny.ocr.application.extractor.OcrTextNormalizationService;
import com.saveapenny.ocr.application.parser.OcrDocumentParser;
import com.saveapenny.ocr.domain.model.OcrTransactionCandidate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RetailReceiptParser implements OcrDocumentParser {

    private final OcrTextNormalizationService normalizationService;
    private final MoneyParser moneyParser;

    public RetailReceiptParser(
            OcrTextNormalizationService normalizationService,
            MoneyParser moneyParser) {
        this.normalizationService = normalizationService;
        this.moneyParser = moneyParser;
    }

    @Override
    public boolean supports(String documentType) {
        return "RETAIL_RECEIPT".equals(documentType);
    }

    @Override
    public List<OcrScoredCandidate> buildCandidates(OcrDocumentAnalysisSeed seed) {
        LocalDate date = seed.paymentDate() != null ? seed.paymentDate() : first(seed.dates());
        if (date == null) {
            return seed.genericCandidates();
        }

        LineAmount chosen = selectRetailAmount(seed.blocks());
        if (chosen == null) {
            return seed.genericCandidates();
        }

        String description = seed.merchantName();
        if (description == null || description.isBlank()) {
            description = firstDescriptiveLine(seed.blocks());
        }
        if (description == null || description.isBlank()) {
            description = chosen.line();
        }

        double confidence = normalizationService.normalizeForMatching(chosen.line()).contains("toplam") ? 0.91 : 0.8;
        return List.of(new OcrScoredCandidate(
                new OcrTransactionCandidate(
                        date,
                        chosen.amount(),
                        normalizationService.normalizeWhitespace(description),
                        detectCategoryHint(description)),
                confidence));
    }

    private LineAmount selectRetailAmount(List<OcrDocumentBlock> blocks) {
        for (OcrDocumentBlock block : blocks) {
            for (String line : block.lines()) {
                String normalized = normalizationService.normalizeForMatching(line);
                if (!normalized.contains("toplam") && !normalized.contains("total")) {
                    continue;
                }
                BigDecimal amount = moneyParser.parseFirst(line);
                if (amount != null) {
                    return new LineAmount(line, amount);
                }
            }
        }

        for (OcrDocumentBlock block : blocks) {
            for (String line : block.lines()) {
                BigDecimal amount = moneyParser.parseFirst(line);
                if (amount != null) {
                    return new LineAmount(line, amount);
                }
            }
        }
        return null;
    }

    private String firstDescriptiveLine(List<OcrDocumentBlock> blocks) {
        for (OcrDocumentBlock block : blocks) {
            for (String line : block.lines()) {
                String normalized = normalizationService.normalizeForMatching(line);
                if (!line.matches(".*\\p{L}.*")) {
                    continue;
                }
                if (normalized.contains("toplam") || normalized.contains("tarih") || normalized.contains("kdv") || normalized.contains("kasiyer")) {
                    continue;
                }
                return line;
            }
        }
        return null;
    }

    private String detectCategoryHint(String line) {
        String normalized = normalizationService.normalizeForMatching(line);
        if (normalized.contains("market") || normalized.contains("grocery") || normalized.contains("restaurant")) {
            return "FOOD";
        }
        if (normalized.contains("uber") || normalized.contains("taxi") || normalized.contains("fuel")) {
            return "TRANSPORT";
        }
        if (normalized.contains("salary") || normalized.contains("payroll")) {
            return "INCOME";
        }
        return "UNCATEGORIZED";
    }

    private <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    private record LineAmount(String line, BigDecimal amount) {
    }
}
