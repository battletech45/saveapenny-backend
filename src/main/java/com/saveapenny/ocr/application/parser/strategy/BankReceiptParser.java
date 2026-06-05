package com.saveapenny.ocr.application.parser.strategy;

import com.saveapenny.ocr.application.analysis.OcrDocumentAnalysisSeed;
import com.saveapenny.ocr.application.analysis.OcrDocumentBlock;
import com.saveapenny.ocr.application.analysis.OcrDocumentBlockType;
import com.saveapenny.ocr.application.analysis.OcrScoredCandidate;
import com.saveapenny.ocr.application.extractor.DateParser;
import com.saveapenny.ocr.application.extractor.MoneyParser;
import com.saveapenny.ocr.application.extractor.OcrTextNormalizationService;
import com.saveapenny.ocr.application.parser.OcrDocumentParser;
import com.saveapenny.ocr.domain.model.OcrTransactionCandidate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BankReceiptParser implements OcrDocumentParser {

    private final OcrTextNormalizationService normalizationService;
    private final DateParser dateParser;
    private final MoneyParser moneyParser;

    public BankReceiptParser(
            OcrTextNormalizationService normalizationService,
            DateParser dateParser,
            MoneyParser moneyParser) {
        this.normalizationService = normalizationService;
        this.dateParser = dateParser;
        this.moneyParser = moneyParser;
    }

    @Override
    public boolean supports(String documentType) {
        return "BANK_RECEIPT".equals(documentType);
    }

    @Override
    public List<OcrScoredCandidate> buildCandidates(OcrDocumentAnalysisSeed seed) {
        LocalDate date = seed.paymentDate() != null ? seed.paymentDate() : first(seed.dates());
        LineAmount totalLine = firstAmountLine(seed.blocks(), OcrDocumentBlockType.TOTALS);
        LineAmount fallbackAmountLine = firstAmountLine(seed.blocks(), OcrDocumentBlockType.DETAIL);
        LineAmount chosenAmount = totalLine != null ? totalLine : fallbackAmountLine;
        if (date == null || chosenAmount == null) {
            return seed.genericCandidates();
        }

        String description = firstDescriptiveLine(seed.blocks(), true);
        if (description == null) {
            description = chosenAmount.line();
        }

        return List.of(new OcrScoredCandidate(
                new OcrTransactionCandidate(
                        date,
                        chosenAmount.amount(),
                        normalizationService.normalizeWhitespace(description),
                        detectCategoryHint(description)),
                chosenAmount == totalLine ? 0.96 : 0.88));
    }

    private LineAmount firstAmountLine(List<OcrDocumentBlock> blocks, OcrDocumentBlockType preferredType) {
        for (OcrDocumentBlock block : blocks) {
            if (block.type() != preferredType) {
                continue;
            }
            for (String line : block.lines()) {
                BigDecimal amount = moneyParser.parseFirst(line);
                if (amount != null) {
                    return new LineAmount(line, amount);
                }
            }
        }
        return null;
    }

    private String firstDescriptiveLine(List<OcrDocumentBlock> blocks, boolean preferDetail) {
        for (OcrDocumentBlock block : blocks) {
            if (preferDetail && block.type() != OcrDocumentBlockType.DETAIL) {
                continue;
            }
            for (String line : block.lines()) {
                String normalized = normalizationService.normalizeForMatching(line);
                if (!line.matches(".*\\p{L}.*")) {
                    continue;
                }
                if (normalized.contains("odeme tarihi") || normalized.contains("duzenlenme tarihi") || normalized.contains("toplam") || normalized.contains("ref no")) {
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
