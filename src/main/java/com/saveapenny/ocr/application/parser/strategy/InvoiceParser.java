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
public class InvoiceParser implements OcrDocumentParser {

    private final OcrTextNormalizationService normalizationService;
    private final MoneyParser moneyParser;

    public InvoiceParser(
            OcrTextNormalizationService normalizationService,
            MoneyParser moneyParser) {
        this.normalizationService = normalizationService;
        this.moneyParser = moneyParser;
    }

    @Override
    public boolean supports(String documentType) {
        return "INVOICE".equals(documentType);
    }

    @Override
    public List<OcrScoredCandidate> buildCandidates(OcrDocumentAnalysisSeed seed) {
        LocalDate date = seed.issueDate() != null ? seed.issueDate() : seed.paymentDate();
        if (date == null && !seed.dates().isEmpty()) {
            date = seed.dates().getFirst();
        }

        LineAmount chosen = selectInvoiceAmount(seed.blocks());
        if (date == null || chosen == null) {
            return seed.genericCandidates();
        }

        String description = seed.merchantName();
        if (description == null || description.isBlank()) {
            description = findInvoiceDescription(seed.blocks());
        }
        if (description == null || description.isBlank()) {
            description = chosen.line();
        }

        return List.of(new OcrScoredCandidate(
                new OcrTransactionCandidate(
                        date,
                        chosen.amount(),
                        normalizationService.normalizeWhitespace(description),
                        "UNCATEGORIZED"),
                0.93));
    }

    private LineAmount selectInvoiceAmount(List<OcrDocumentBlock> blocks) {
        for (OcrDocumentBlock block : blocks) {
            for (String line : block.lines()) {
                String normalized = normalizationService.normalizeForMatching(line);
                if (!normalized.contains("toplam") && !normalized.contains("total") && !normalized.contains("genel toplam")) {
                    continue;
                }
                BigDecimal amount = moneyParser.parseFirst(line);
                if (amount != null) {
                    return new LineAmount(line, amount);
                }
            }
        }
        return null;
    }

    private String findInvoiceDescription(List<OcrDocumentBlock> blocks) {
        for (OcrDocumentBlock block : blocks) {
            for (String line : block.lines()) {
                String normalized = normalizationService.normalizeForMatching(line);
                if (!line.matches(".*\\p{L}.*")) {
                    continue;
                }
                if (normalized.contains("invoice") || normalized.contains("fatura") || normalized.contains("toplam") || normalized.contains("date")) {
                    continue;
                }
                return line;
            }
        }
        return null;
    }

    private record LineAmount(String line, BigDecimal amount) {
    }
}
