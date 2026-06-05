package com.saveapenny.ocr.application.parser.strategy;

import com.saveapenny.ocr.application.analysis.OcrDocumentAnalysisSeed;
import com.saveapenny.ocr.application.analysis.OcrDocumentBlock;
import com.saveapenny.ocr.application.analysis.OcrDocumentBlockType;
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
public class TaxReceiptParser implements OcrDocumentParser {

    private final OcrTextNormalizationService normalizationService;
    private final MoneyParser moneyParser;

    public TaxReceiptParser(
            OcrTextNormalizationService normalizationService,
            MoneyParser moneyParser) {
        this.normalizationService = normalizationService;
        this.moneyParser = moneyParser;
    }

    @Override
    public boolean supports(String documentType) {
        return "TAX_RECEIPT".equals(documentType);
    }

    @Override
    public List<OcrScoredCandidate> buildCandidates(OcrDocumentAnalysisSeed seed) {
        LocalDate date = seed.paymentDate() != null ? seed.paymentDate() : seed.issueDate();
        if (date == null && !seed.dates().isEmpty()) {
            date = seed.dates().getFirst();
        }

        BigDecimal amount = null;
        String amountLine = null;
        for (OcrDocumentBlock block : seed.blocks()) {
            for (String line : block.lines()) {
                String normalized = normalizationService.normalizeForMatching(line);
                BigDecimal parsed = moneyParser.parseFirst(line);
                if (parsed == null) {
                    continue;
                }
                if (normalized.contains("toplam")) {
                    amount = parsed;
                    amountLine = line;
                    break;
                }
                if (amount == null) {
                    amount = parsed;
                    amountLine = line;
                }
            }
            if (amountLine != null && normalizationService.normalizeForMatching(amountLine).contains("toplam")) {
                break;
            }
        }

        String description = findTaxDescription(seed.blocks());
        if (date == null || amount == null) {
            return seed.genericCandidates();
        }

        double confidence = normalizationService.normalizeForMatching(amountLine).contains("toplam") ? 0.95 : 0.84;
        return List.of(new OcrScoredCandidate(
                new OcrTransactionCandidate(
                        date,
                        amount,
                        normalizationService.normalizeWhitespace(description != null ? description : amountLine),
                        "UNCATEGORIZED"),
                confidence));
    }

    private String findTaxDescription(List<OcrDocumentBlock> blocks) {
        for (OcrDocumentBlock block : blocks) {
            if (block.type() == OcrDocumentBlockType.HEADER || block.type() == OcrDocumentBlockType.FOOTER) {
                continue;
            }
            for (String line : block.lines()) {
                String normalized = normalizationService.normalizeForMatching(line);
                if (!line.matches(".*\\p{L}.*")) {
                    continue;
                }
                if (normalized.contains("vergi tahsil") || normalized.contains("mukellef") || normalized.contains("odeme tarihi")) {
                    continue;
                }
                if (normalized.contains("harc") || normalized.contains("vergi") || normalized.contains("nevi")) {
                    if (normalized.contains("toplam") || normalized.contains("odeme tarihi")) {
                        continue;
                    }
                    return line;
                }
            }
        }
        return null;
    }
}
